use futures_util::{SinkExt, StreamExt};
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use tokio::net::{TcpListener, UdpSocket};
use tokio::sync::{mpsc, Mutex};
use tokio_tungstenite::accept_async;
use tokio_tungstenite::tungstenite::Message;
use tracing::{debug, error, info, warn};

use crate::audio::{SystemAudioCapture, VirtualMicrophone};
use crate::config::Config;
use crate::input::{VirtualKeyboard, VirtualMouse};
use crate::protocol::*;

pub struct ServerState {
    pub config: Config,
    pub mouse: Option<VirtualMouse>,
    pub keyboard: Option<VirtualKeyboard>,
    pub virtual_mic: Option<VirtualMicrophone>,
    pub system_capture: Option<SystemAudioCapture>,
    pub mouse_enabled: bool,
    pub keyboard_enabled: bool,
    pub mic_enabled: bool,
    pub speaker_enabled: bool,
    pub client_udp_addr: Option<SocketAddr>,
    pub pairing_token: Option<String>,
}

impl ServerState {
    pub fn new(pairing_token: Option<String>) -> Self {
        let mouse = VirtualMouse::new()
            .map_err(|e| {
                warn!(
                    "Failed to initialize virtual mouse: {}. Running without mouse.",
                    e
                );
                e
            })
            .ok();

        let keyboard = VirtualKeyboard::new()
            .map_err(|e| {
                warn!(
                    "Failed to initialize virtual keyboard: {}. Running without keyboard.",
                    e
                );
                e
            })
            .ok();

        Self {
            config: Config::load(),
            mouse,
            keyboard,
            virtual_mic: None,
            system_capture: None,
            mouse_enabled: true,
            keyboard_enabled: true,
            mic_enabled: false,
            speaker_enabled: false,
            client_udp_addr: None,
            pairing_token,
        }
    }
}

pub struct PluglessServer {
    state: Arc<Mutex<ServerState>>,
    ws_port: u16,
    udp_port: u16,
}

impl PluglessServer {
    pub fn new(ws_port: u16, udp_port: u16, pairing_token: Option<String>) -> Self {
        Self {
            state: Arc::new(Mutex::new(ServerState::new(pairing_token))),
            ws_port,
            udp_port,
        }
    }

    pub async fn run(self: Arc<Self>) -> Result<(), Box<dyn std::error::Error>> {
        let ws_addr = format!("0.0.0.0:{}", self.ws_port);
        let ws_listener = TcpListener::bind(&ws_addr).await?;
        info!("🚀 Plugless Control Server listening on ws://{}", ws_addr);

        let udp_addr = format!("0.0.0.0:{}", self.udp_port);
        let udp_socket = Arc::new(UdpSocket::bind(&udp_addr).await?);
        info!("🔊 Plugless Audio Streamer listening on udp://{}", udp_addr);

        // Spawn UDP Audio Receiver Task (handles incoming mic stream from phone)
        let udp_rx = udp_socket.clone();
        let state_udp = self.state.clone();
        tokio::spawn(async move {
            let mut buf = vec![0u8; 4096];
            loop {
                match udp_rx.recv_from(&mut buf).await {
                    Ok((len, src_addr)) => {
                        if len < 8 {
                            continue;
                        }
                        let mut state = state_udp.lock().await;
                        state.client_udp_addr = Some(src_addr);

                        let packet_type = buf[0];
                        match packet_type {
                            AUDIO_PACKET_MIC => {
                                if state.mic_enabled {
                                    if let Some(ref mut mic) = state.virtual_mic {
                                        let pcm_payload = &buf[8..len];
                                        let _ = mic.write_pcm(pcm_payload);
                                    }
                                }
                            }
                            AUDIO_PACKET_PING => {
                                let mut pong = vec![0u8; len];
                                pong.copy_from_slice(&buf[0..len]);
                                pong[0] = AUDIO_PACKET_PONG;
                                let _ = udp_rx.send_to(&pong, src_addr).await;
                            }
                            _ => {}
                        }
                    }
                    Err(e) => {
                        error!("UDP receive error: {}", e);
                        tokio::time::sleep(Duration::from_millis(100)).await;
                    }
                }
            }
        });

        // Accept WebSocket connections
        while let Ok((stream, peer_addr)) = ws_listener.accept().await {
            info!("Incoming connection from: {}", peer_addr);
            let state_clone = self.state.clone();
            let udp_tx = udp_socket.clone();
            let audio_port = self.udp_port;

            tokio::spawn(async move {
                if let Err(e) =
                    Self::handle_ws_client(stream, peer_addr, state_clone, udp_tx, audio_port).await
                {
                    warn!("Client {} disconnected: {}", peer_addr, e);
                }
            });
        }

        Ok(())
    }

    async fn handle_ws_client(
        stream: tokio::net::TcpStream,
        peer_addr: SocketAddr,
        state: Arc<Mutex<ServerState>>,
        udp_socket: Arc<UdpSocket>,
        audio_port: u16,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let ws_stream = accept_async(stream).await?;
        let (mut ws_sender, mut ws_receiver) = ws_stream.split();

        let mut authenticated = false;
        let (speaker_pcm_tx, mut speaker_pcm_rx) = mpsc::channel::<Vec<u8>>(100);

        // Task to transmit speaker audio from PC to phone over UDP
        let udp_speaker = udp_socket.clone();
        let state_speaker = state.clone();
        let speaker_task = tokio::spawn(async move {
            let mut seq: u16 = 0;
            while let Some(pcm_chunk) = speaker_pcm_rx.recv().await {
                let target_addr = {
                    let st = state_speaker.lock().await;
                    if !st.speaker_enabled {
                        continue;
                    }
                    st.client_udp_addr
                };

                if let Some(addr) = target_addr {
                    let now_ms = (SystemTime::now()
                        .duration_since(UNIX_EPOCH)
                        .unwrap()
                        .as_millis()
                        & 0xFFFFFFFF) as u32;
                    let mut packet = Vec::with_capacity(8 + pcm_chunk.len());
                    packet.push(AUDIO_PACKET_SPEAKER);
                    packet.push(0); // flags
                    packet.extend_from_slice(&seq.to_be_bytes());
                    packet.extend_from_slice(&now_ms.to_be_bytes());
                    packet.extend_from_slice(&pcm_chunk);

                    seq = seq.wrapping_add(1);
                    let _ = udp_speaker.send_to(&packet, addr).await;
                }
            }
        });

        // Loop reading WebSocket messages
        while let Some(msg_result) = ws_receiver.next().await {
            let msg = msg_result?;
            if msg.is_text() {
                let text = msg.to_text()?;
                let client_msg: ClientMessage = match serde_json::from_str(text) {
                    Ok(m) => m,
                    Err(e) => {
                        debug!("Invalid client message: {}", e);
                        continue;
                    }
                };

                if !authenticated {
                    if let ClientMessage::Auth { token, device_name } = client_msg {
                        let mut st = state.lock().await;
                        let token_valid = if let Some(ref pairing_token) = st.pairing_token {
                            &token == pairing_token
                        } else {
                            st.config.is_trusted(&token).is_some()
                        };

                        if token_valid {
                            authenticated = true;
                            info!("  ✓ Device authenticated: {}", device_name);
                            st.config.add_or_update_device(
                                peer_addr.ip().to_string(),
                                device_name,
                                token.clone(),
                            );

                            let resp = ServerMessage::AuthResponse {
                                success: true,
                                message: "Authenticated successfully".to_string(),
                                audio_port,
                            };
                            let resp_text = serde_json::to_string(&resp)?;
                            ws_sender.send(Message::Text(resp_text)).await?;

                            // Send initial state sync
                            let sync = ServerMessage::StateSync {
                                mouse: st.mouse_enabled,
                                keyboard: st.keyboard_enabled,
                                mic: st.mic_enabled,
                                speaker: st.speaker_enabled,
                                mic_virtual_name: Some("Plugless_Virtual_Mic".to_string()),
                                speaker_monitor_name: Some("@DEFAULT_MONITOR@".to_string()),
                            };
                            ws_sender
                                .send(Message::Text(serde_json::to_string(&sync)?))
                                .await?;
                        } else {
                            warn!("Unauthorized connection attempt with token: {}", token);
                            let resp = ServerMessage::AuthResponse {
                                success: false,
                                message: "Invalid or expired pairing token".to_string(),
                                audio_port: 0,
                            };
                            ws_sender
                                .send(Message::Text(serde_json::to_string(&resp)?))
                                .await?;
                            break;
                        }
                    } else {
                        // Reject unauthenticated command
                        continue;
                    }
                } else {
                    // Authenticated operations
                    let mut st = state.lock().await;
                    match client_msg {
                        ClientMessage::MouseMove { dx, dy } => {
                            if st.mouse_enabled {
                                if let Some(ref mut m) = st.mouse {
                                    let _ = m.move_relative(dx, dy);
                                }
                            }
                        }
                        ClientMessage::MouseClick {
                            button,
                            state: btn_state,
                        } => {
                            if st.mouse_enabled {
                                if let Some(ref mut m) = st.mouse {
                                    let _ = m.click(button, btn_state);
                                }
                            }
                        }
                        ClientMessage::MouseScroll { dx, dy } => {
                            if st.mouse_enabled {
                                if let Some(ref mut m) = st.mouse {
                                    let _ = m.scroll(dx, dy);
                                }
                            }
                        }
                        ClientMessage::KeyPress {
                            key_code,
                            state: key_state,
                        } => {
                            if st.keyboard_enabled {
                                if let Some(ref mut kb) = st.keyboard {
                                    let _ = kb.send_key(key_code, key_state);
                                }
                            }
                        }
                        ClientMessage::KeyText { text } => {
                            if st.keyboard_enabled {
                                if let Some(ref mut kb) = st.keyboard {
                                    let _ = kb.type_text(&text);
                                }
                            }
                        }
                        ClientMessage::SpecialKey {
                            key,
                            state: key_state,
                        } => {
                            if st.keyboard_enabled {
                                if let Some(ref mut kb) = st.keyboard {
                                    let _ = kb.send_special_key(&key, key_state);
                                }
                            }
                        }
                        ClientMessage::SetToolState { tool, enabled } => {
                            match tool {
                                ToolType::Mouse => {
                                    st.mouse_enabled = enabled;
                                    info!("Mouse tool toggled: {}", enabled);
                                }
                                ToolType::Keyboard => {
                                    st.keyboard_enabled = enabled;
                                    info!("Keyboard tool toggled: {}", enabled);
                                }
                                ToolType::Mic => {
                                    st.mic_enabled = enabled;
                                    if enabled && st.virtual_mic.is_none() {
                                        st.virtual_mic = VirtualMicrophone::new().ok();
                                    } else if !enabled {
                                        st.virtual_mic = None;
                                    }
                                    info!("Microphone tool toggled: {}", enabled);
                                }
                                ToolType::Speaker => {
                                    st.speaker_enabled = enabled;
                                    if enabled && st.system_capture.is_none() {
                                        st.system_capture =
                                            SystemAudioCapture::start(speaker_pcm_tx.clone()).ok();
                                    } else if !enabled {
                                        st.system_capture = None;
                                    }
                                    info!("Speaker tool toggled: {}", enabled);
                                }
                            }

                            // Broadcast updated state
                            let sync = ServerMessage::StateSync {
                                mouse: st.mouse_enabled,
                                keyboard: st.keyboard_enabled,
                                mic: st.mic_enabled,
                                speaker: st.speaker_enabled,
                                mic_virtual_name: Some("Plugless_Virtual_Mic".to_string()),
                                speaker_monitor_name: Some("@DEFAULT_MONITOR@".to_string()),
                            };
                            ws_sender
                                .send(Message::Text(serde_json::to_string(&sync)?))
                                .await?;
                        }
                        ClientMessage::Ping { client_ts } => {
                            let server_ts = SystemTime::now()
                                .duration_since(UNIX_EPOCH)
                                .unwrap()
                                .as_millis() as u64;
                            let pong = ServerMessage::Pong {
                                client_ts,
                                server_ts,
                            };
                            ws_sender
                                .send(Message::Text(serde_json::to_string(&pong)?))
                                .await?;
                        }
                        ClientMessage::Auth { .. } => {}
                    }
                }
            } else if msg.is_close() {
                break;
            }
        }

        speaker_task.abort();
        info!("Client connection finished: {}", peer_addr);
        Ok(())
    }
}

#![allow(dead_code)]
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ToolType {
    Mouse,
    Keyboard,
    Mic,
    Speaker,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum MouseButton {
    Left,
    Right,
    Middle,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ButtonState {
    Down,
    Up,
    Click,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum KeyState {
    Down,
    Up,
    Click,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum ClientMessage {
    Auth {
        token: String,
        device_name: String,
    },
    MouseMove {
        dx: f32,
        dy: f32,
    },
    MouseClick {
        button: MouseButton,
        state: ButtonState,
    },
    MouseScroll {
        dx: f32,
        dy: f32,
    },
    KeyPress {
        key_code: u16,
        state: KeyState,
    },
    KeyText {
        text: String,
    },
    SpecialKey {
        key: String,
        state: KeyState,
    },
    SetToolState {
        tool: ToolType,
        enabled: bool,
    },
    Ping {
        client_ts: u64,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum ServerMessage {
    AuthResponse {
        success: bool,
        message: String,
        audio_port: u16,
    },
    StateSync {
        mouse: bool,
        keyboard: bool,
        mic: bool,
        speaker: bool,
        mic_virtual_name: Option<String>,
        speaker_monitor_name: Option<String>,
    },
    Pong {
        client_ts: u64,
        server_ts: u64,
    },
    Error {
        code: String,
        message: String,
    },
}

/// UDP Audio packet types
pub const AUDIO_PACKET_MIC: u8 = 0x01; // Phone Mic -> PC Virtual Mic
pub const AUDIO_PACKET_SPEAKER: u8 = 0x02; // PC Speaker -> Phone
pub const AUDIO_PACKET_PING: u8 = 0x03; // Latency probe
pub const AUDIO_PACKET_PONG: u8 = 0x04;

pub const DEFAULT_WS_PORT: u16 = 8765;
pub const DEFAULT_UDP_PORT: u16 = 8766;

/// Audio constants: 48000Hz, 16-bit PCM (standard broadcast quality)
pub const SAMPLE_RATE: u32 = 48000;
pub const MIC_CHANNELS: u16 = 1;
pub const SPEAKER_CHANNELS: u16 = 2;
pub const FRAME_DURATION_MS: u32 = 20; // 20ms chunks for low latency
pub const MIC_CHUNK_SAMPLES: usize = (SAMPLE_RATE * FRAME_DURATION_MS / 1000) as usize; // 960 samples
pub const MIC_CHUNK_BYTES: usize = MIC_CHUNK_SAMPLES * 2; // 1920 bytes
pub const SPEAKER_CHUNK_SAMPLES: usize = MIC_CHUNK_SAMPLES * SPEAKER_CHANNELS as usize; // 1920 samples
pub const SPEAKER_CHUNK_BYTES: usize = SPEAKER_CHUNK_SAMPLES * 2; // 3840 bytes

use std::io::{self, Read};
use std::process::{Command, Stdio};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::thread;
use tokio::sync::mpsc;
use tracing::{info, warn};

pub struct SystemAudioCapture {
    running: Arc<AtomicBool>,
    worker_handle: Option<thread::JoinHandle<()>>,
}

impl SystemAudioCapture {
    pub fn start(pcm_sender: mpsc::Sender<Vec<u8>>) -> io::Result<Self> {
        info!("Starting System Audio Capture (Speaker Streamer)...");

        let monitor = Self::get_default_monitor_source();
        info!("Capturing PC audio from monitor source: {}", monitor);

        let child = Command::new("parec")
            .args([
                "--format=s16le",
                "--rate=48000",
                "--channels=2",
                "--latency-msec=20",
                "-d",
                &monitor,
            ])
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .spawn()
            .or_else(|_| {
                info!("Fallback to default monitor without -d flag");
                Command::new("parec")
                    .args([
                        "--format=s16le",
                        "--rate=48000",
                        "--channels=2",
                        "--latency-msec=20",
                    ])
                    .stdout(Stdio::piped())
                    .stderr(Stdio::null())
                    .spawn()
            })?;

        let running = Arc::new(AtomicBool::new(true));
        let running_clone = running.clone();

        let worker_handle = thread::spawn(move || {
            // 20ms of stereo 16-bit 48kHz audio = 48000 * 0.02 * 2 channels * 2 bytes = 3840 bytes
            let mut buf = vec![0u8; 3840];
            let mut current_child = child;

            while running_clone.load(Ordering::Relaxed) {
                if let Some(ref mut stdout) = current_child.stdout {
                    match stdout.read_exact(&mut buf) {
                        Ok(()) => {
                            if pcm_sender.blocking_send(buf.clone()).is_err() {
                                break;
                            }
                        }
                        Err(e) => {
                            if !running_clone.load(Ordering::Relaxed) {
                                break;
                            }
                            warn!("parec stream ended: {}. Reconnecting...", e);
                            thread::sleep(std::time::Duration::from_millis(200));

                            let mon = Self::get_default_monitor_source();
                            if let Ok(new_child) = Command::new("parec")
                                .args([
                                    "--format=s16le",
                                    "--rate=48000",
                                    "--channels=2",
                                    "--latency-msec=20",
                                    "-d",
                                    &mon,
                                ])
                                .stdout(Stdio::piped())
                                .stderr(Stdio::null())
                                .spawn()
                            {
                                current_child = new_child;
                                info!("parec reconnected successfully to {}", mon);
                            } else {
                                thread::sleep(std::time::Duration::from_millis(500));
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            let _ = current_child.kill();
        });

        Ok(Self {
            running,
            worker_handle: Some(worker_handle),
        })
    }

    fn get_default_monitor_source() -> String {
        if let Ok(output) = Command::new("pactl").args(["get-default-sink"]).output() {
            let sink = String::from_utf8_lossy(&output.stdout).trim().to_string();
            if !sink.is_empty() {
                return format!("{}.monitor", sink);
            }
        }
        "@DEFAULT_MONITOR@".to_string()
    }
}

impl Drop for SystemAudioCapture {
    fn drop(&mut self) {
        self.running.store(false, Ordering::Relaxed);
        if let Some(handle) = self.worker_handle.take() {
            let _ = handle.join();
        }
        info!("Stopped System Audio Capture.");
    }
}

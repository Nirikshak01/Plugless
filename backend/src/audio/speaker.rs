use std::io::{self, Read};
use std::process::{Child, Command, Stdio};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::thread;
use tokio::sync::mpsc;
use tracing::{debug, info};

pub struct SystemAudioCapture {
    running: Arc<AtomicBool>,
    parec_process: Option<Child>,
    worker_handle: Option<thread::JoinHandle<()>>,
}

impl SystemAudioCapture {
    pub fn start(pcm_sender: mpsc::Sender<Vec<u8>>) -> io::Result<Self> {
        info!("Starting System Audio Capture (Speaker Streamer)...");

        // Use parec to record from the default monitor source
        let mut child = Command::new("parec")
            .args([
                "--format=s16le",
                "--rate=48000",
                "--channels=2",
                "--latency-msec=20",
                "-d",
                "@DEFAULT_MONITOR@",
            ])
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .spawn()
            .or_else(|_| {
                // Fallback to default sink monitor explicitly
                let default_monitor = Self::get_default_monitor_source();
                Command::new("parec")
                    .args([
                        "--format=s16le",
                        "--rate=48000",
                        "--channels=2",
                        "--latency-msec=20",
                        "-d",
                        &default_monitor,
                    ])
                    .stdout(Stdio::piped())
                    .stderr(Stdio::null())
                    .spawn()
            })?;

        let mut stdout = child
            .stdout
            .take()
            .ok_or_else(|| io::Error::other("Failed to capture parec stdout"))?;

        let running = Arc::new(AtomicBool::new(true));
        let running_clone = running.clone();

        let worker_handle = thread::spawn(move || {
            // 20ms of stereo 16-bit 48kHz audio = 48000 * 0.02 * 2 channels * 2 bytes = 3840 bytes
            let mut buf = vec![0u8; 3840];
            while running_clone.load(Ordering::Relaxed) {
                match stdout.read_exact(&mut buf) {
                    Ok(()) => {
                        if pcm_sender.blocking_send(buf.clone()).is_err() {
                            break;
                        }
                    }
                    Err(e) => {
                        debug!("parec read stream closed: {}", e);
                        break;
                    }
                }
            }
        });

        Ok(Self {
            running,
            parec_process: Some(child),
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
        if let Some(ref mut child) = self.parec_process {
            let _ = child.kill();
        }
        if let Some(handle) = self.worker_handle.take() {
            let _ = handle.join();
        }
        info!("Stopped System Audio Capture.");
    }
}

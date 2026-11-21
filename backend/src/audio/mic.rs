use std::io::{self, Write};
use std::process::{Child, Command, Stdio};
use tracing::{info, warn};

pub struct VirtualMicrophone {
    sink_module_id: Option<String>,
    source_module_id: Option<String>,
    pacat_process: Option<Child>,
}

impl VirtualMicrophone {
    pub fn new() -> io::Result<Self> {
        info!("Setting up Plugless Virtual Microphone via PulseAudio / PipeWire...");

        // Unload any existing stale Plugless modules first
        Self::cleanup_stale_modules();

        // 1. Create Null Sink for Microphone
        let sink_id = match Command::new("pactl")
            .args([
                "load-module",
                "module-null-sink",
                "sink_name=Plugless_Mic",
                "sink_properties=device.description=\"Plugless_Virtual_Microphone\"",
            ])
            .output()
        {
            Ok(output) if output.status.success() => {
                let id = String::from_utf8_lossy(&output.stdout).trim().to_string();
                info!("  ✓ Created null sink 'Plugless_Mic' (module id: {})", id);
                Some(id)
            }
            Ok(output) => {
                warn!(
                    "Failed to create null sink: {}",
                    String::from_utf8_lossy(&output.stderr)
                );
                None
            }
            Err(e) => {
                warn!("pactl command not available: {}", e);
                None
            }
        };

        // 2. Create Remap Source so apps recognize it as an Input / Recording device
        let source_id = if sink_id.is_some() {
            match Command::new("pactl")
                .args([
                    "load-module",
                    "module-remap-source",
                    "master=Plugless_Mic.monitor",
                    "source_name=Plugless_Virtual_Mic",
                    "source_properties=device.description=\"Plugless_Microphone\"",
                ])
                .output()
            {
                Ok(output) if output.status.success() => {
                    let id = String::from_utf8_lossy(&output.stdout).trim().to_string();
                    info!(
                        "  ✓ Created remap source 'Plugless_Virtual_Mic' (module id: {})",
                        id
                    );
                    let _ = Command::new("pactl")
                        .args(["set-default-source", "Plugless_Virtual_Mic"])
                        .output();
                    info!("  ✓ Set 'Plugless_Virtual_Mic' as active system default source");
                    Some(id)
                }
                Ok(output) => {
                    warn!(
                        "Failed to remap source: {}. Falling back to monitor sink.",
                        String::from_utf8_lossy(&output.stderr)
                    );
                    let _ = Command::new("pactl")
                        .args(["set-default-source", "Plugless_Mic.monitor"])
                        .output();
                    None
                }
                Err(e) => {
                    warn!("Failed to execute pactl remap: {}", e);
                    let _ = Command::new("pactl")
                        .args(["set-default-source", "Plugless_Mic.monitor"])
                        .output();
                    None
                }
            }
        } else {
            None
        };

        // 3. Spawn pacat process to feed incoming PCM stream into Plugless_Mic
        let pacat_process = match Command::new("pacat")
            .args([
                "--playback",
                "-d",
                "Plugless_Mic",
                "--format=s16le",
                "--rate=48000",
                "--channels=1",
                "--latency-msec=20",
            ])
            .stdin(Stdio::piped())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .spawn()
        {
            Ok(child) => {
                info!("  ✓ Spawned pacat audio playback pipeline");
                Some(child)
            }
            Err(e) => {
                warn!("Could not spawn pacat for virtual microphone: {}", e);
                None
            }
        };

        Ok(Self {
            sink_module_id: sink_id,
            source_module_id: source_id,
            pacat_process,
        })
    }

    pub fn write_pcm(&mut self, pcm_data: &[u8]) -> io::Result<()> {
        if let Some(ref mut child) = self.pacat_process {
            if let Some(ref mut stdin) = child.stdin {
                stdin.write_all(pcm_data)?;
                stdin.flush()?;
            }
        }
        Ok(())
    }

    fn cleanup_stale_modules() {
        if let Ok(output) = Command::new("pactl")
            .args(["list", "short", "modules"])
            .output()
        {
            let stdout = String::from_utf8_lossy(&output.stdout);
            for line in stdout.lines() {
                if line.contains("Plugless_Mic") || line.contains("Plugless_Virtual_Mic") {
                    if let Some(id) = line.split_whitespace().next() {
                        let _ = Command::new("pactl").args(["unload-module", id]).output();
                    }
                }
            }
        }
    }
}

impl Drop for VirtualMicrophone {
    fn drop(&mut self) {
        if let Some(ref mut child) = self.pacat_process {
            let _ = child.kill();
        }
        if let Some(ref id) = self.source_module_id {
            let _ = Command::new("pactl").args(["unload-module", id]).output();
        }
        if let Some(ref id) = self.sink_module_id {
            let _ = Command::new("pactl").args(["unload-module", id]).output();
        }
        info!("Cleaned up Plugless Virtual Microphone modules.");
    }
}

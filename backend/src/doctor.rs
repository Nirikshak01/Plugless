use std::fs::OpenOptions;
use std::net::{TcpListener, UdpSocket};
use std::process::Command;

pub struct DoctorCheck {
    pub name: &'static str,
    pub passed: bool,
    pub details: String,
    pub fix_recommendation: Option<String>,
}

pub fn run_doctor() -> Vec<DoctorCheck> {
    let mut checks = Vec::new();

    // 1. Check /dev/uinput access
    let uinput_accessible = OpenOptions::new()
        .read(true)
        .write(true)
        .open("/dev/uinput")
        .or_else(|_| {
            OpenOptions::new()
                .read(true)
                .write(true)
                .open("/dev/input/uinput")
        })
        .is_ok();

    if uinput_accessible {
        checks.push(DoctorCheck {
            name: "Virtual Input (/dev/uinput)",
            passed: true,
            details: "Read/Write permissions granted for virtual mouse & keyboard.".to_string(),
            fix_recommendation: None,
        });
    } else {
        checks.push(DoctorCheck {
            name: "Virtual Input (/dev/uinput)",
            passed: false,
            details: "Cannot access /dev/uinput. Root permissions or 'input' group required.".to_string(),
            fix_recommendation: Some(
                "Run: sudo usermod -aG input $USER && echo 'KERNEL==\"uinput\", MODE=\"0660\", GROUP=\"input\"' | sudo tee /etc/udev/rules.d/99-uinput.rules && sudo udevadm trigger".to_string()
            ),
        });
    }

    // 2. Check PulseAudio / PipeWire sound server
    let audio_check = Command::new("pactl").args(["info"]).output();
    match audio_check {
        Ok(out) if out.status.success() => {
            let info = String::from_utf8_lossy(&out.stdout);
            let server_name = info
                .lines()
                .find(|l| l.starts_with("Server Name:"))
                .unwrap_or("Server Name: Active")
                .to_string();
            checks.push(DoctorCheck {
                name: "Sound Architecture (PipeWire / PulseAudio)",
                passed: true,
                details: format!("Connected to sound server: {}", server_name),
                fix_recommendation: None,
            });
        }
        _ => {
            checks.push(DoctorCheck {
                name: "Sound Architecture (PipeWire / PulseAudio)",
                passed: false,
                details: "Could not connect to PipeWire or PulseAudio daemon via pactl.".to_string(),
                fix_recommendation: Some(
                    "Ensure pipewire-pulse or pulseaudio is active: systemctl --user start pipewire-pulse".to_string(),
                ),
            });
        }
    }

    // 3. Audio tools: pacat and parec
    let pacat_found = Command::new("which")
        .arg("pacat")
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false);
    let parec_found = Command::new("which")
        .arg("parec")
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false);

    if pacat_found && parec_found {
        checks.push(DoctorCheck {
            name: "Audio Streaming Utilities (pacat & parec)",
            passed: true,
            details: "Both pacat (virtual mic playback) and parec (system capture) are installed."
                .to_string(),
            fix_recommendation: None,
        });
    } else {
        checks.push(DoctorCheck {
            name: "Audio Streaming Utilities (pacat & parec)",
            passed: false,
            details: "Missing pacat or parec for virtual audio routing.".to_string(),
            fix_recommendation: Some(
                "Install pulse utilities: sudo pacman -S libpulse (Arch) or sudo apt install pulseaudio-utils (Ubuntu/Debian)".to_string(),
            ),
        });
    }

    // 4. Local IP detection
    match local_ip_address::local_ip() {
        Ok(ip) => {
            checks.push(DoctorCheck {
                name: "Local Network Interface",
                passed: true,
                details: format!("Detected LAN IP address: {}", ip),
                fix_recommendation: None,
            });
        }
        Err(e) => {
            checks.push(DoctorCheck {
                name: "Local Network Interface",
                passed: false,
                details: format!("Failed to resolve local LAN IP address: {}", e),
                fix_recommendation: Some(
                    "Check WiFi/Ethernet connection to your local router.".to_string(),
                ),
            });
        }
    }

    // 5. Port availability: 8765 (WebSocket) and 8766 (UDP Audio)
    let ws_available = TcpListener::bind("0.0.0.0:8765").is_ok();
    let udp_available = UdpSocket::bind("0.0.0.0:8766").is_ok();

    if ws_available && udp_available {
        checks.push(DoctorCheck {
            name: "Network Ports (8765 TCP, 8766 UDP)",
            passed: true,
            details: "Both ports are open and ready for peripheral communication.".to_string(),
            fix_recommendation: None,
        });
    } else {
        checks.push(DoctorCheck {
            name: "Network Ports (8765 TCP, 8766 UDP)",
            passed: false,
            details: format!(
                "Port conflict: TCP 8765 free = {}, UDP 8766 free = {}",
                ws_available, udp_available
            ),
            fix_recommendation: Some(
                "Ensure another instance of Plugless is not already running.".to_string(),
            ),
        });
    }

    // 6. Virtual Camera device (/dev/video*) for native Zoom, Discord & Web Browsers
    let v4l2_devices: Vec<String> = (0..16)
        .filter_map(|i| {
            let path = format!("/dev/video{}", i);
            if std::path::Path::new(&path).exists() {
                Some(path)
            } else {
                None
            }
        })
        .collect();

    if !v4l2_devices.is_empty() {
        checks.push(DoctorCheck {
            name: "Virtual Camera (V4L2 Device)",
            passed: true,
            details: format!("Found video device(s): {}. Ready for native webcam.", v4l2_devices.join(", ")),
            fix_recommendation: None,
        });
    } else {
        checks.push(DoctorCheck {
            name: "Virtual Camera (V4L2 Loopback for Web Browsers & Zoom)",
            passed: false,
            details: "No /dev/video* devices found. Native browser/Zoom direct webcam requires v4l2loopback (MJPEG streaming still works via OBS Studio).".to_string(),
            fix_recommendation: Some(
                "To enable direct virtual webcam in browsers & Zoom: sudo modprobe v4l2loopback video_nr=2 card_label=\"Plugless_Webcam\" exclusive_caps=1 (Arch: sudo pacman -S v4l2loopback-dkms; Ubuntu: sudo apt install v4l2loopback-dkms)".to_string(),
            ),
        });
    }

    checks
}

pub fn print_doctor_report(checks: &[DoctorCheck]) -> bool {
    println!("\n  🩺  Plugless System Diagnostics Doctor");
    println!("  ─────────────────────────────────────────────────────────────");

    let mut all_passed = true;
    for check in checks {
        if check.passed {
            println!("  \x1b[32m[✓]\x1b[0m \x1b[1m{}\x1b[0m", check.name);
            println!("      \x1b[90m{}\x1b[0m", check.details);
        } else {
            all_passed = false;
            println!("  \x1b[31m[✗]\x1b[0m \x1b[1m{}\x1b[0m", check.name);
            println!("      {}", check.details);
            if let Some(ref fix) = check.fix_recommendation {
                println!("      \x1b[33m💡 Fix: {}\x1b[0m", fix);
            }
        }
    }

    println!("  ─────────────────────────────────────────────────────────────");
    if all_passed {
        println!("  \x1b[32m✨ All system requirements satisfied! Plugless is ready.\x1b[0m\n");
    } else {
        println!(
            "  \x1b[33m⚠️  Some checks failed. Follow the fix recommendations above.\x1b[0m\n"
        );
    }

    all_passed
}

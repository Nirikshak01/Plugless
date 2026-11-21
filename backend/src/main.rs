use clap::Parser;
use qrcode::render::unicode;
use qrcode::QrCode;
use rand::Rng;
use serde_json::json;
use std::sync::Arc;
use tracing::Level;
use tracing_subscriber::FmtSubscriber;

use plugless::cli::{Cli, Commands};
use plugless::config::Config;
use plugless::doctor::{print_doctor_report, run_doctor};
use plugless::server::PluglessServer;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    let command = cli.command.unwrap_or(Commands::Pair {
        port: 8765,
        audio_port: 8766,
    });

    match command {
        Commands::Doctor => {
            let checks = run_doctor();
            let all_passed = print_doctor_report(&checks);
            if !all_passed {
                std::process::exit(1);
            }
        }

        Commands::Devices => {
            let config = Config::load();
            println!("\n  📱  Plugless Trusted Devices");
            println!("  ─────────────────────────────────────────────────────────────");
            if config.trusted_devices.is_empty() {
                println!("  No trusted devices paired yet.");
                println!("  Run 'plugless pair' to pair your mobile phone.\n");
            } else {
                for dev in &config.trusted_devices {
                    let last_seen_dt = chrono_like(dev.last_seen);
                    println!(
                        "  • \x1b[1m{}\x1b[0m (ID: \x1b[36m{}\x1b[0m)",
                        dev.name, dev.id
                    );
                    println!("    Last active: {}", last_seen_dt);
                }
                println!();
            }
        }

        Commands::Forget { device } => {
            let mut config = Config::load();
            if config.remove_device(&device) {
                println!("  ✓ Successfully forgot device: '{}'", device);
            } else {
                println!("  ✗ Device '{}' not found in trusted devices list.", device);
            }
        }

        Commands::Status => {
            let config = Config::load();
            println!("\n  ⚡  Plugless Server Status");
            println!("  ─────────────────────────────────────────────────────────────");
            println!("  Version:           1.0.0");
            println!("  Default WS Port:   8765");
            println!("  Default UDP Port:  8766");
            println!("  Paired Devices:    {}", config.trusted_devices.len());
            let local_ip = local_ip_address::local_ip()
                .map(|ip| ip.to_string())
                .unwrap_or_else(|_| "Unknown".to_string());
            println!("  LAN IP:            {}", local_ip);
            println!("  ─────────────────────────────────────────────────────────────\n");
        }

        Commands::Pair { port, audio_port } => {
            setup_logging();

            let local_ip = local_ip_address::local_ip()
                .map(|ip| ip.to_string())
                .unwrap_or_else(|_| "127.0.0.1".to_string());

            let token: String = rand::thread_rng()
                .sample_iter(&rand::distributions::Alphanumeric)
                .take(16)
                .map(char::from)
                .collect();

            let payload = json!({
                "proto": "plugless",
                "version": "1.0.0",
                "ip": local_ip,
                "port": port,
                "audio_port": audio_port,
                "token": token
            })
            .to_string();

            print_banner(&local_ip, port, &token);

            if let Ok(code) = QrCode::new(payload.as_bytes()) {
                let qr_render = code.render::<unicode::Dense1x2>().build();
                println!("{}", qr_render);
            }

            println!("  \x1b[32m✔ Scan this QR code with the Plugless Android App to connect instantly!\x1b[0m");
            println!("  \x1b[90m(Press Ctrl+C to stop)\x1b[0m\n");

            let server = Arc::new(PluglessServer::new(port, audio_port, Some(token)));
            server.run().await?;
        }

        Commands::Daemon { port, audio_port } => {
            setup_logging();
            let local_ip = local_ip_address::local_ip()
                .map(|ip| ip.to_string())
                .unwrap_or_else(|_| "127.0.0.1".to_string());

            println!(
                "\n  ⚡ Starting Plugless Background Daemon on ws://{}:{}...",
                local_ip, port
            );
            let server = Arc::new(PluglessServer::new(port, audio_port, None));
            server.run().await?;
        }
    }

    Ok(())
}

fn setup_logging() {
    let subscriber = FmtSubscriber::builder()
        .with_max_level(Level::INFO)
        .with_target(false)
        .finish();
    let _ = tracing::subscriber::set_global_default(subscriber);
}

fn print_banner(ip: &str, port: u16, token: &str) {
    println!("\x1b[36m");
    println!("  ██████╗ ██╗     ██╗   ██╗ ██████╗ ██╗     ███████╗███████╗███████╗");
    println!("  ██╔══██╗██║     ██║   ██║██╔════╝ ██║     ██╔════╝██╔════╝██╔════╝");
    println!("  ██████╔╝██║     ██║   ██║██║  ███╗██║     █████╗  ███████╗███████╗");
    println!("  ██╔═══╝ ██║     ██║   ██║██║   ██║██║     ██╔══╝  ╚════██║╚════██║");
    println!("  ██║     ███████╗╚██████╔╝╚██████╔╝███████╗███████╗███████║███████║");
    println!("  ╚═╝     ╚══════╝ ╚═════╝  ╚═════╝ ╚══════╝╚══════╝╚══════╝╚══════╝");
    println!("\x1b[0m");
    println!("  Turn your phone into zero-wire PC peripherals (Mouse, Keyboard, Mic, Speaker)");
    println!("  ─────────────────────────────────────────────────────────────");
    println!("  Host IP:         \x1b[1mhttp://{}:{}\x1b[0m", ip, port);
    println!("  Pairing Token:   \x1b[33m{}\x1b[0m", token);
    println!("  ─────────────────────────────────────────────────────────────\n");
}

fn chrono_like(ts: u64) -> String {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs();
    let diff = now.saturating_sub(ts);
    if diff < 60 {
        "Just now".to_string()
    } else if diff < 3600 {
        format!("{} mins ago", diff / 60)
    } else if diff < 86400 {
        format!("{} hours ago", diff / 3600)
    } else {
        format!("{} days ago", diff / 86400)
    }
}

use clap::{Parser, Subcommand};

#[derive(Parser, Debug)]
#[command(name = "plugless")]
#[command(author = "Nirikshak <thakorjasmin503@gmail.com>")]
#[command(version = "1.0.0")]
#[command(about = "Turn your phone into zero-wire PC peripherals (mouse, keyboard, microphone, speaker)", long_about = None)]
pub struct Cli {
    #[command(subcommand)]
    pub command: Option<Commands>,
}

#[derive(Subcommand, Debug)]
pub enum Commands {
    /// Generate a pairing QR code in terminal and wait for phone connection
    Pair {
        #[arg(short, long, default_value_t = 8765)]
        port: u16,

        #[arg(short, long, default_value_t = 8766)]
        audio_port: u16,
    },

    /// Run the persistent background peripheral daemon
    Daemon {
        #[arg(short, long, default_value_t = 8765)]
        port: u16,

        #[arg(short, long, default_value_t = 8766)]
        audio_port: u16,
    },

    /// Check Plugless server status and active peripherals
    Status,

    /// List all paired and trusted mobile devices
    Devices,

    /// Forget/revoke a trusted mobile device by ID or name
    Forget { device: String },

    /// Run system diagnostics and verify Linux permissions (uinput, PipeWire, ports)
    Doctor,
}

#![allow(dead_code)]
use serde::{Deserialize, Serialize};
use std::fs::{self, File};
use std::io::{self, Read, Write};
use std::path::PathBuf;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrustedDevice {
    pub id: String,
    pub name: String,
    pub token: String,
    pub paired_at: u64,
    pub last_seen: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct Config {
    pub trusted_devices: Vec<TrustedDevice>,
    pub port: Option<u16>,
    pub audio_port: Option<u16>,
}

impl Config {
    pub fn config_path() -> PathBuf {
        let mut path = dirs::config_dir().unwrap_or_else(|| PathBuf::from("."));
        path.push("plugless");
        path.push("config.json");
        path
    }

    pub fn load() -> Self {
        let path = Self::config_path();
        if path.exists() {
            if let Ok(mut file) = File::open(&path) {
                let mut content = String::new();
                if file.read_to_string(&mut content).is_ok() {
                    if let Ok(config) = serde_json::from_str(&content) {
                        return config;
                    }
                }
            }
        }
        Self::default()
    }

    pub fn save(&self) -> io::Result<()> {
        let path = Self::config_path();
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)?;
        }
        let json = serde_json::to_string_pretty(self)?;
        let mut file = File::create(path)?;
        file.write_all(json.as_bytes())?;
        Ok(())
    }

    pub fn add_or_update_device(&mut self, id: String, name: String, token: String) {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        if let Some(dev) = self
            .trusted_devices
            .iter_mut()
            .find(|d| d.id == id || d.token == token)
        {
            dev.name = name;
            dev.token = token;
            dev.last_seen = now;
        } else {
            self.trusted_devices.push(TrustedDevice {
                id,
                name,
                token,
                paired_at: now,
                last_seen: now,
            });
        }
        let _ = self.save();
    }

    pub fn remove_device(&mut self, query: &str) -> bool {
        let initial_len = self.trusted_devices.len();
        self.trusted_devices
            .retain(|d| d.id != query && d.name != query);
        let removed = self.trusted_devices.len() < initial_len;
        if removed {
            let _ = self.save();
        }
        removed
    }

    pub fn is_trusted(&self, token: &str) -> Option<&TrustedDevice> {
        self.trusted_devices.iter().find(|d| d.token == token)
    }

    pub fn update_last_seen(&mut self, token: &str) {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();
        if let Some(dev) = self.trusted_devices.iter_mut().find(|d| d.token == token) {
            dev.last_seen = now;
            let _ = self.save();
        }
    }
}

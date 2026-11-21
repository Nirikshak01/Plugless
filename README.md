# Plugless

Turn your Android smartphone into zero-wire PC peripherals (mouse, keyboard, microphone, speaker) over local WiFi.

## Why "Plugless"?
Plugless turns your mobile phone into zero-wire PC peripherals (mouse, keyboard, microphone, speaker) without requiring any cables, dongles, or audio splitters.

---

## ⚡ 1-Line Terminal Install (Linux)

```bash
curl -fsSL https://raw.githubusercontent.com/Nirikshak01/Plugless/main/install.sh | bash
```

This installs:
- The standalone `plugless` binary to `~/.local/bin`
- Linux desktop launcher in your Applications menu
- High-resolution app icons
- Systemd user service (`plugless.service`)

---

## 📱 Android Application

Download the release APK directly to your phone:
- **[👉 Download Plugless Android APK](https://github.com/Nirikshak01/Plugless/releases/latest/download/Plugless.apk)**

Requirements: Android 8.0+ (API 26+). Works entirely offline over local WiFi.

---

## 🚀 Quick Usage Guide

1. **On your Linux PC**, launch pairing mode:
   ```bash
   plugless pair
   ```
   An ASCII QR code will appear in your terminal.

2. **On your Phone**, open the Plugless app and tap **Scan PC**.
   Scan the terminal QR code. Pairing completes instantly.

3. **Select Your Peripherals**:
   - 🖱️ **Mouse**: 1-finger tracking, tap to click, 2-finger scroll, long-press drag
   - ⌨️ **Keyboard**: Soft keyboard text typing, modifier keys (Ctrl, Alt, Super), navigation arrows
   - 🎙️ **Microphone**: Phone mic appears on PC as a real virtual recording device (Zoom, Discord, OBS, WebRTC)
   - 🔊 **Speaker**: PC system audio streams in real time to your phone speakers or headphones

4. **Combo Mode**:
   Toggle any combination simultaneously. Run both Mic + Speaker for full-duplex calls with echo cancellation and noise suppression.

---

## 🛠️ CLI Commands

| Command | Description |
|---|---|
| `plugless pair` | Displays pairing QR code and waits for phone connection |
| `plugless daemon` | Runs the background peripheral service |
| `plugless doctor` | Validates Linux uinput and PipeWire permissions |
| `plugless status` | Shows connection status, active tools, and local IP |
| `plugless devices` | Lists trusted paired devices |
| `plugless forget <device>` | Revokes access for a trusted device |

---

## 🏗️ Architecture Overview

- **Input Emulation**: Native Linux `/dev/uinput` virtual devices (compositor-agnostic, works on both Wayland and X11).
- **Virtual Audio**: Dynamically manages PipeWire / PulseAudio null sinks and remap sources.
- **Multiplexed Transport**: WebSocket on port 8765 for <15ms control latency; UDP on port 8766 for 20ms audio frames.
- **Security**: Ephemeral pairing tokens, local LAN scoping, no cloud dependencies.

Detailed documentation:
- [Architecture Details](docs/ARCHITECTURE.md)
- [Protocol Specification](docs/PROTOCOL.md)

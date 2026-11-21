# Plugless

Turn your Android smartphone into zero-wire PC peripherals (**Mouse, Keyboard, Virtual Microphone, PC Speaker, and PC Webcam**) over local WiFi.

## Why "Plugless"?
Plugless eliminates cables, USB dongles, and hardware adapters by turning the hardware you already own (your smartphone's touchscreen, keyboard, microphone, speaker, and camera) into plug-and-play Linux PC peripherals.

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
- **[👉 Download Plugless Android APK (v1.0.0)](https://github.com/Nirikshak01/Plugless/releases/latest/download/Plugless.apk)**

Requirements: Android 8.0+ (API 26+). Works entirely offline over local WiFi.

---

## 🚀 Quick Usage Guide

1. **On your Linux PC**, launch pairing mode:
   ```bash
   plugless pair
   ```
   A high-contrast ANSI QR code and direct connection details will appear in your terminal.

2. **On your Phone**, open the Plugless app and tap **Scan**.
   - Point your camera at the terminal QR code — Google ML Kit detects it instantly.
   - Alternatively, tap **"Enter IP Manually"** to connect directly via IP.
   - Once connected, the pill button shows **● Connected**.

3. **Select & Activate Your Peripherals**:
   - Tap any peripheral card to open its dedicated control screen.
   - Toggle the **ON / OFF switch** at the top right to start that tool.
   - Tap Back (`←`) to return to Home — the tile clearly displays **● ON** in green!
   - You can turn on multiple tools simultaneously (e.g. Mic + Speaker + Camera + Mouse) and run all of them together in the background!

### 🛠️ Peripherals Included:
- 🖱️ **Mouse & Touchpad**: 1-finger tracking, tap to click, 2-finger scroll, long-press drag lock, sensitivity slider.
- ⌨️ **PC Keyboard**: Text typing, modifier keys (Ctrl, Alt, Shift, Super), special keys (Esc, Tab, Enter, Backspace), navigation arrows, and common shortcuts.
- 🎙️ **Virtual Microphone**: Phone mic streams in real-time to a Linux PipeWire/PulseAudio virtual microphone (`Plugless_Virtual_Mic`) detected by Zoom, Discord, OBS, and browsers. Includes Noise Suppression and Echo Cancellation.
- 🔊 **PC Speaker**: PC system audio streams with ultra-low latency to your phone's speaker or headphones.
- 📷 **PC Webcam (Camera)**: Streams phone camera video to your PC. Open `http://localhost:8767/video` in OBS Studio or any browser for a crystal-clear HD virtual webcam!

---

## 🛠️ CLI Commands

| Command | Description |
|---|---|
| `plugless pair` | Displays pairing QR code and waits for phone connection |
| `plugless daemon` | Runs the background peripheral service |
| `plugless doctor` | Runs system diagnostics (uinput, PipeWire, ports, permissions) |
| `plugless status` | Displays server state, active tools, and local network IP |
| `plugless devices` | Lists paired trusted mobile devices |
| `plugless forget <device>` | Revokes and forgets a paired phone |

---

## 📄 License
MIT License. Created by [Nirikshak](https://github.com/Nirikshak01).

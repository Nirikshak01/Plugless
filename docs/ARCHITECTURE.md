# Plugless Architecture & Engineering Specification

Plugless transforms an Android smartphone into high-performance, low-latency PC peripherals:
- **Touchpad Mouse** (relative tracking, left/right/middle click, 2-finger scroll)
- **Virtual Keyboard** (system IME text streaming, modifier keys, arrow navigation pad)
- **Virtual Microphone** (system-wide PipeWire / PulseAudio recording source recognized by Zoom, Discord, OBS, WebRTC)
- **PC Speaker Streamer** (captures default monitor output and streams audio to mobile headphones/speakers)

---

## 1. Multi-Channel Multiplexing & Transport Rationale

A core design requirement is **Combo Mode**: running mouse, keyboard, microphone, and speaker concurrently without audio packet queues causing cursor stutter.

### Dual-Channel Separation:
1. **Control Channel (WebSocket over TCP - Port 8765)**:
   - Delivers low-volume, latency-critical input events (mouse delta coordinates, keystrokes, tool toggles, authentication, keep-alive pings).
   - TCP ensures 100% reliable delivery for modifier keys, text typing, and state handshakes.
   - Sub-millisecond async dispatch via Tokio keeps input latency strictly under **15ms** on typical 5GHz WiFi networks.

2. **Audio Channel (UDP Datagrams - Port 8766)**:
   - Voice and system audio require bounded latency rather than guaranteed delivery.
   - Re-transmissions inherent in TCP head-of-line blocking cause voice stutter and delay spikes.
   - Audio is packetized into **20ms frames** (960 samples @ 48kHz, 16-bit PCM).
   - Each datagram has an 8-byte header: `[packet_type (1B), flags (1B), sequence_number (2B), timestamp_ms (4B)]` followed by PCM payload.
   - If WiFi packet jitter occurs, late audio frames are dropped without delaying pointer coordinates.

---

## 2. Linux Input Subsystem (uinput)

Plugless interfaces directly with the Linux kernel `/dev/uinput` character device without intermediary user-space X11/Wayland wrapper libraries (`xdotool`, `ydotool`).

- **Wayland & X11 Universal Compatibility**: Kernel-level virtual devices are compositor-agnostic. They work seamlessly on GNOME, KDE Plasma, Sway, Hyprland, XFCE, and headless configurations.
- **Mouse Emulation**: Registers `EV_REL` with `REL_X`, `REL_Y`, `REL_WHEEL`, `REL_HWHEEL`, and `EV_KEY` with `BTN_LEFT`, `BTN_RIGHT`, `BTN_MIDDLE`. Fractional coordinate accumulators ensure sub-pixel finger drags translate to natural cursor momentum.
- **Keyboard Emulation**: Registers `EV_KEY` with scan codes 1..248, supporting modifier chaining (Ctrl, Alt, Shift, Super) and ASCII mapping.

---

## 3. Linux Audio Architecture (PipeWire & PulseAudio)

Plugless establishes virtual audio nodes dynamically using native PipeWire / PulseAudio control APIs:

```
[Phone Mic] ---> UDP (Port 8766) ---> Plugless Backend ---> pacat ---> [Plugless_Mic (Null Sink)]
                                                                               │
                                                                   [Plugless_Virtual_Mic (Source)]
                                                                               │
                                                             Seen by Zoom, Discord, OBS, Browsers

[PC Audio Output] ---> [Default Sink Monitor] ---> parec ---> UDP (Port 8766) ---> [Phone Speaker]
```

1. **Virtual Microphone**:
   - `pactl load-module module-null-sink sink_name=Plugless_Mic sink_properties=device.description="Plugless_Virtual_Microphone"`
   - `pactl load-module module-remap-source master=Plugless_Mic.monitor source_name=Plugless_Virtual_Mic source_properties=device.description="Plugless_Microphone"`
   - Audio received from the Android microphone is written via `pacat --playback` into `Plugless_Mic`. Any communication app selecting `Plugless_Microphone` receives voice with zero additional setup.
   - On daemon shutdown, modules are cleanly unloaded.

2. **Speaker Capture**:
   - Captures from `@DEFAULT_MONITOR@` using `parec --latency-msec=20`.
   - Audio chunks are transmitted to the phone's UDP socket for real-time playback via `AudioTrack`.

---

## 4. Security & Zero-Trust Pairing

- **One-Time Pairing Secret**: Every `plugless pair` session generates an ephemeral, cryptographically random 16-character alphanumeric token rendered inside the terminal QR code.
- **Local Network Scoped**: No external relay or cloud servers are used. All packets travel strictly within the user's LAN.
- **Trusted Device Persistence**: Successful pairing stores authorized devices in `~/.config/plugless/config.json`.
- **Revocation**: Devices can be inspected with `plugless devices` and revoked with `plugless forget <device_id>`.

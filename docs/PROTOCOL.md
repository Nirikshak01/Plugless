# Plugless Protocol Specification

Plugless utilizes a dual-transport model:
1. Control Transport: WebSocket (TCP) on port 8765
2. Audio Transport: UDP on port 8766

## 1. QR Code Pairing Payload

When running `plugless pair`, the terminal outputs a QR code containing JSON:

```json
{
  "proto": "plugless",
  "version": "1.0.0",
  "ip": "192.168.1.100",
  "port": 8765,
  "audio_port": 8766,
  "token": "RANDOM_TOKEN_STRING"
}
```

## 2. WebSocket Control Frames

### Client -> Server

- Auth: `{"type": "auth", "token": "...", "device_name": "..."}`
- Mouse Move: `{"type": "mouse_move", "dx": 10.0, "dy": -5.0}`
- Mouse Click: `{"type": "mouse_click", "button": "left", "state": "click"}`
- Mouse Scroll: `{"type": "mouse_scroll", "dx": 0.0, "dy": 2.0}`
- Key Press: `{"type": "key_press", "key_code": 28, "state": "click"}`
- Key Text: `{"type": "key_text", "text": "hello"}`
- Special Key: `{"type": "special_key", "key": "ctrl", "state": "down"}`
- Set Tool State: `{"type": "set_tool_state", "tool": "mic", "enabled": true}`
- Ping: `{"type": "ping", "client_ts": 1732172400000}`

### Server -> Client

- Auth Response: `{"type": "auth_response", "success": true, "message": "...", "audio_port": 8766}`
- State Sync: `{"type": "state_sync", "mouse": true, "keyboard": true, "mic": false, "speaker": false}`
- Pong: `{"type": "pong", "client_ts": 1732172400000, "server_ts": 1732172400004}`

## 3. UDP Audio Packet Format

Header (8 bytes):
- Byte 0: Packet type (0x01 = Mic from phone, 0x02 = Speaker to phone, 0x03 = Ping, 0x04 = Pong)
- Byte 1: Flags (reserved, 0x00)
- Bytes 2-3: Sequence number (16-bit big-endian integer)
- Bytes 4-7: Timestamp in milliseconds (32-bit big-endian integer)
- Remaining Bytes: 48kHz 16-bit PCM samples

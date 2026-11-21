use crate::input::uinput_raw::{UInputDevice, EV_KEY};
use crate::protocol::KeyState;
use std::io;
use tracing::debug;

// Standard Linux input key codes (linux/input-event-codes.h)
pub const KEY_ESC: u16 = 1;
pub const KEY_1: u16 = 2;
pub const KEY_2: u16 = 3;
pub const KEY_3: u16 = 4;
pub const KEY_4: u16 = 5;
pub const KEY_5: u16 = 6;
pub const KEY_6: u16 = 7;
pub const KEY_7: u16 = 8;
pub const KEY_8: u16 = 9;
pub const KEY_9: u16 = 10;
pub const KEY_0: u16 = 11;
pub const KEY_MINUS: u16 = 12;
pub const KEY_EQUAL: u16 = 13;
pub const KEY_BACKSPACE: u16 = 14;
pub const KEY_TAB: u16 = 15;
pub const KEY_Q: u16 = 16;
pub const KEY_W: u16 = 17;
pub const KEY_E: u16 = 18;
pub const KEY_R: u16 = 19;
pub const KEY_T: u16 = 20;
pub const KEY_Y: u16 = 21;
pub const KEY_U: u16 = 22;
pub const KEY_I: u16 = 23;
pub const KEY_O: u16 = 24;
pub const KEY_P: u16 = 25;
pub const KEY_LEFTBRACE: u16 = 26;
pub const KEY_RIGHTBRACE: u16 = 27;
pub const KEY_ENTER: u16 = 28;
pub const KEY_LEFTCTRL: u16 = 29;
pub const KEY_A: u16 = 30;
pub const KEY_S: u16 = 31;
pub const KEY_D: u16 = 32;
pub const KEY_F: u16 = 33;
pub const KEY_G: u16 = 34;
pub const KEY_H: u16 = 35;
pub const KEY_J: u16 = 36;
pub const KEY_K: u16 = 37;
pub const KEY_L: u16 = 38;
pub const KEY_SEMICOLON: u16 = 39;
pub const KEY_APOSTROPHE: u16 = 40;
pub const KEY_GRAVE: u16 = 41;
pub const KEY_LEFTSHIFT: u16 = 42;
pub const KEY_BACKSLASH: u16 = 43;
pub const KEY_Z: u16 = 44;
pub const KEY_X: u16 = 45;
pub const KEY_C: u16 = 46;
pub const KEY_V: u16 = 47;
pub const KEY_B: u16 = 48;
pub const KEY_N: u16 = 49;
pub const KEY_M: u16 = 50;
pub const KEY_COMMA: u16 = 51;
pub const KEY_DOT: u16 = 52;
pub const KEY_SLASH: u16 = 53;
pub const KEY_RIGHTSHIFT: u16 = 54;
pub const KEY_LEFTALT: u16 = 56;
pub const KEY_SPACE: u16 = 57;
pub const KEY_CAPSLOCK: u16 = 58;
pub const KEY_F1: u16 = 59;
pub const KEY_F2: u16 = 60;
pub const KEY_F3: u16 = 61;
pub const KEY_F4: u16 = 62;
pub const KEY_F5: u16 = 63;
pub const KEY_F6: u16 = 64;
pub const KEY_F7: u16 = 65;
pub const KEY_F8: u16 = 66;
pub const KEY_F9: u16 = 67;
pub const KEY_F10: u16 = 68;
pub const KEY_F11: u16 = 87;
pub const KEY_F12: u16 = 88;
pub const KEY_RIGHTCTRL: u16 = 97;
pub const KEY_RIGHTALT: u16 = 100;
pub const KEY_HOME: u16 = 102;
pub const KEY_UP: u16 = 103;
pub const KEY_PAGEUP: u16 = 104;
pub const KEY_LEFT: u16 = 105;
pub const KEY_RIGHT: u16 = 106;
pub const KEY_END: u16 = 107;
pub const KEY_DOWN: u16 = 108;
pub const KEY_PAGEDOWN: u16 = 109;
pub const KEY_INSERT: u16 = 110;
pub const KEY_DELETE: u16 = 111;
pub const KEY_LEFTMETA: u16 = 125; // Super / Windows key

pub struct VirtualKeyboard {
    device: UInputDevice,
}

impl VirtualKeyboard {
    pub fn new() -> io::Result<Self> {
        let device = UInputDevice::create_keyboard("Plugless Virtual Keyboard")?;
        Ok(Self { device })
    }

    pub fn send_key(&mut self, key_code: u16, state: KeyState) -> io::Result<()> {
        match state {
            KeyState::Down => {
                self.device.emit(EV_KEY, key_code, 1)?;
                self.device.sync()?;
            }
            KeyState::Up => {
                self.device.emit(EV_KEY, key_code, 0)?;
                self.device.sync()?;
            }
            KeyState::Click => {
                self.device.emit(EV_KEY, key_code, 1)?;
                self.device.sync()?;
                self.device.emit(EV_KEY, key_code, 0)?;
                self.device.sync()?;
            }
        }
        Ok(())
    }

    pub fn send_special_key(&mut self, key_name: &str, state: KeyState) -> io::Result<()> {
        if let Some(code) = Self::map_special_key(key_name) {
            self.send_key(code, state)
        } else {
            debug!("Unknown special key: {}", key_name);
            Ok(())
        }
    }

    pub fn type_text(&mut self, text: &str) -> io::Result<()> {
        for ch in text.chars() {
            if let Some((code, shift)) = Self::char_to_key(ch) {
                if shift {
                    self.device.emit(EV_KEY, KEY_LEFTSHIFT, 1)?;
                    self.device.sync()?;
                }
                self.device.emit(EV_KEY, code, 1)?;
                self.device.sync()?;
                self.device.emit(EV_KEY, code, 0)?;
                self.device.sync()?;
                if shift {
                    self.device.emit(EV_KEY, KEY_LEFTSHIFT, 0)?;
                    self.device.sync()?;
                }
            }
        }
        Ok(())
    }

    fn map_special_key(name: &str) -> Option<u16> {
        match name.to_lowercase().as_str() {
            "esc" | "escape" => Some(KEY_ESC),
            "tab" => Some(KEY_TAB),
            "enter" | "return" => Some(KEY_ENTER),
            "backspace" => Some(KEY_BACKSPACE),
            "space" => Some(KEY_SPACE),
            "ctrl" | "leftctrl" | "control" => Some(KEY_LEFTCTRL),
            "rightctrl" => Some(KEY_RIGHTCTRL),
            "alt" | "leftalt" => Some(KEY_LEFTALT),
            "rightalt" | "altgr" => Some(KEY_RIGHTALT),
            "shift" | "leftshift" => Some(KEY_LEFTSHIFT),
            "rightshift" => Some(KEY_RIGHTSHIFT),
            "super" | "win" | "meta" => Some(KEY_LEFTMETA),
            "capslock" => Some(KEY_CAPSLOCK),
            "up" | "arrowup" => Some(KEY_UP),
            "down" | "arrowdown" => Some(KEY_DOWN),
            "left" | "arrowleft" => Some(KEY_LEFT),
            "right" | "arrowright" => Some(KEY_RIGHT),
            "pageup" => Some(KEY_PAGEUP),
            "pagedown" => Some(KEY_PAGEDOWN),
            "home" => Some(KEY_HOME),
            "end" => Some(KEY_END),
            "insert" => Some(KEY_INSERT),
            "delete" | "del" => Some(KEY_DELETE),
            "f1" => Some(KEY_F1),
            "f2" => Some(KEY_F2),
            "f3" => Some(KEY_F3),
            "f4" => Some(KEY_F4),
            "f5" => Some(KEY_F5),
            "f6" => Some(KEY_F6),
            "f7" => Some(KEY_F7),
            "f8" => Some(KEY_F8),
            "f9" => Some(KEY_F9),
            "f10" => Some(KEY_F10),
            "f11" => Some(KEY_F11),
            "f12" => Some(KEY_F12),
            _ => None,
        }
    }

    fn char_to_key(ch: char) -> Option<(u16, bool)> {
        match ch {
            'a'..='z' => {
                let offset = (ch as u16) - ('a' as u16);
                let codes = [
                    KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I, KEY_J, KEY_K,
                    KEY_L, KEY_M, KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S, KEY_T, KEY_U, KEY_V,
                    KEY_W, KEY_X, KEY_Y, KEY_Z,
                ];
                Some((codes[offset as usize], false))
            }
            'A'..='Z' => {
                let offset = (ch as u16) - ('A' as u16);
                let codes = [
                    KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I, KEY_J, KEY_K,
                    KEY_L, KEY_M, KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S, KEY_T, KEY_U, KEY_V,
                    KEY_W, KEY_X, KEY_Y, KEY_Z,
                ];
                Some((codes[offset as usize], true))
            }
            '1' => Some((KEY_1, false)),
            '!' => Some((KEY_1, true)),
            '2' => Some((KEY_2, false)),
            '@' => Some((KEY_2, true)),
            '3' => Some((KEY_3, false)),
            '#' => Some((KEY_3, true)),
            '4' => Some((KEY_4, false)),
            '$' => Some((KEY_4, true)),
            '5' => Some((KEY_5, false)),
            '%' => Some((KEY_5, true)),
            '6' => Some((KEY_6, false)),
            '^' => Some((KEY_6, true)),
            '7' => Some((KEY_7, false)),
            '&' => Some((KEY_7, true)),
            '8' => Some((KEY_8, false)),
            '*' => Some((KEY_8, true)),
            '9' => Some((KEY_9, false)),
            '(' => Some((KEY_9, true)),
            '0' => Some((KEY_0, false)),
            ')' => Some((KEY_0, true)),
            '-' => Some((KEY_MINUS, false)),
            '_' => Some((KEY_MINUS, true)),
            '=' => Some((KEY_EQUAL, false)),
            '+' => Some((KEY_EQUAL, true)),
            '\n' => Some((KEY_ENTER, false)),
            '\t' => Some((KEY_TAB, false)),
            ' ' => Some((KEY_SPACE, false)),
            '[' => Some((KEY_LEFTBRACE, false)),
            '{' => Some((KEY_LEFTBRACE, true)),
            ']' => Some((KEY_RIGHTBRACE, false)),
            '}' => Some((KEY_RIGHTBRACE, true)),
            ';' => Some((KEY_SEMICOLON, false)),
            ':' => Some((KEY_SEMICOLON, true)),
            '\'' => Some((KEY_APOSTROPHE, false)),
            '"' => Some((KEY_APOSTROPHE, true)),
            '`' => Some((KEY_GRAVE, false)),
            '~' => Some((KEY_GRAVE, true)),
            '\\' => Some((KEY_BACKSLASH, false)),
            '|' => Some((KEY_BACKSLASH, true)),
            ',' => Some((KEY_COMMA, false)),
            '<' => Some((KEY_COMMA, true)),
            '.' => Some((KEY_DOT, false)),
            '>' => Some((KEY_DOT, true)),
            '/' => Some((KEY_SLASH, false)),
            '?' => Some((KEY_SLASH, true)),
            _ => None,
        }
    }
}

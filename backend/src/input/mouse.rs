use crate::input::uinput_raw::{
    UInputDevice, BTN_LEFT, BTN_MIDDLE, BTN_RIGHT, EV_KEY, EV_REL, REL_HWHEEL, REL_WHEEL, REL_X,
    REL_Y,
};
use crate::protocol::{ButtonState, MouseButton};
use std::io;

pub struct VirtualMouse {
    device: UInputDevice,
    acc_x: f32,
    acc_y: f32,
}

impl VirtualMouse {
    pub fn new() -> io::Result<Self> {
        let device = UInputDevice::create_mouse("Plugless Virtual Mouse")?;
        Ok(Self {
            device,
            acc_x: 0.0,
            acc_y: 0.0,
        })
    }

    pub fn move_relative(&mut self, dx: f32, dy: f32) -> io::Result<()> {
        self.acc_x += dx;
        self.acc_y += dy;

        let ix = self.acc_x as i32;
        let iy = self.acc_y as i32;

        if ix != 0 || iy != 0 {
            self.acc_x -= ix as f32;
            self.acc_y -= iy as f32;

            if ix != 0 {
                self.device.emit(EV_REL, REL_X, ix)?;
            }
            if iy != 0 {
                self.device.emit(EV_REL, REL_Y, iy)?;
            }
            self.device.sync()?;
        }
        Ok(())
    }

    pub fn click(&mut self, button: MouseButton, state: ButtonState) -> io::Result<()> {
        let btn_code = match button {
            MouseButton::Left => BTN_LEFT,
            MouseButton::Right => BTN_RIGHT,
            MouseButton::Middle => BTN_MIDDLE,
        };

        match state {
            ButtonState::Down => {
                self.device.emit(EV_KEY, btn_code, 1)?;
                self.device.sync()?;
            }
            ButtonState::Up => {
                self.device.emit(EV_KEY, btn_code, 0)?;
                self.device.sync()?;
            }
            ButtonState::Click => {
                self.device.emit(EV_KEY, btn_code, 1)?;
                self.device.sync()?;
                self.device.emit(EV_KEY, btn_code, 0)?;
                self.device.sync()?;
            }
        }
        Ok(())
    }

    pub fn scroll(&mut self, dx: f32, dy: f32) -> io::Result<()> {
        // Vertical scroll
        if dy.abs() >= 1.0 {
            let steps = dy.round() as i32;
            self.device.emit(EV_REL, REL_WHEEL, steps)?;
        }
        // Horizontal scroll
        if dx.abs() >= 1.0 {
            let steps = dx.round() as i32;
            self.device.emit(EV_REL, REL_HWHEEL, steps)?;
        }
        self.device.sync()
    }
}

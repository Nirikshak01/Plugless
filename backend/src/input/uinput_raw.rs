#![allow(dead_code)]
use std::fs::{File, OpenOptions};
use std::io::{self, Write};
use std::mem;
use std::os::unix::fs::OpenOptionsExt;
use std::os::unix::io::{AsRawFd, RawFd};

// Linux input constants
pub const EV_SYN: u16 = 0x00;
pub const EV_KEY: u16 = 0x01;
pub const EV_REL: u16 = 0x02;

pub const SYN_REPORT: u16 = 0;

pub const REL_X: u16 = 0x00;
pub const REL_Y: u16 = 0x01;
pub const REL_HWHEEL: u16 = 0x06;
pub const REL_WHEEL: u16 = 0x08;

pub const BTN_LEFT: u16 = 0x110;
pub const BTN_RIGHT: u16 = 0x111;
pub const BTN_MIDDLE: u16 = 0x112;

pub const BUS_USB: u16 = 0x03;

// Ioctls
const UI_DEV_CREATE: libc::c_ulong = 0x5501;
const UI_DEV_DESTROY: libc::c_ulong = 0x5502;
const UI_DEV_SETUP: libc::c_ulong = 0x405c5503;
const UI_SET_EVBIT: libc::c_ulong = 0x40045564;
const UI_SET_KEYBIT: libc::c_ulong = 0x40045565;
const UI_SET_RELBIT: libc::c_ulong = 0x40045566;

#[repr(C)]
struct InputId {
    bustype: u16,
    vendor: u16,
    product: u16,
    version: u16,
}

#[repr(C)]
struct UInputSetup {
    id: InputId,
    name: [libc::c_char; 80],
    ff_effects_max: u32,
}

#[repr(C)]
struct InputEvent {
    time: libc::timeval,
    type_: u16,
    code: u16,
    value: i32,
}

pub struct UInputDevice {
    file: File,
    name: String,
}

impl UInputDevice {
    pub fn open() -> io::Result<File> {
        OpenOptions::new()
            .read(true)
            .write(true)
            .custom_flags(libc::O_NONBLOCK)
            .open("/dev/uinput")
            .or_else(|_| {
                OpenOptions::new()
                    .read(true)
                    .write(true)
                    .custom_flags(libc::O_NONBLOCK)
                    .open("/dev/input/uinput")
            })
    }

    pub fn create_mouse(name: &str) -> io::Result<Self> {
        let file = Self::open()?;
        let fd = file.as_raw_fd();

        unsafe {
            // Enable Event Types: SYN, KEY, REL
            Self::ioctl(fd, UI_SET_EVBIT, EV_SYN as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_EVBIT, EV_KEY as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_EVBIT, EV_REL as libc::c_ulong)?;

            // Relative axes: X, Y, WHEEL, HWHEEL
            Self::ioctl(fd, UI_SET_RELBIT, REL_X as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_RELBIT, REL_Y as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_RELBIT, REL_WHEEL as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_RELBIT, REL_HWHEEL as libc::c_ulong)?;

            // Mouse Buttons
            Self::ioctl(fd, UI_SET_KEYBIT, BTN_LEFT as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_KEYBIT, BTN_RIGHT as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_KEYBIT, BTN_MIDDLE as libc::c_ulong)?;

            // Setup device
            Self::setup_device(fd, name, 0x1234, 0x5678)?;
        }

        Ok(Self {
            file,
            name: name.to_string(),
        })
    }

    pub fn create_keyboard(name: &str) -> io::Result<Self> {
        let file = Self::open()?;
        let fd = file.as_raw_fd();

        unsafe {
            Self::ioctl(fd, UI_SET_EVBIT, EV_SYN as libc::c_ulong)?;
            Self::ioctl(fd, UI_SET_EVBIT, EV_KEY as libc::c_ulong)?;

            // Enable all key codes from 1 to 248 (covers all standard keyboard keys)
            for key_code in 1..248 {
                Self::ioctl(fd, UI_SET_KEYBIT, key_code as libc::c_ulong)?;
            }

            Self::setup_device(fd, name, 0x1234, 0x5679)?;
        }

        Ok(Self {
            file,
            name: name.to_string(),
        })
    }

    unsafe fn setup_device(fd: RawFd, name: &str, vendor: u16, product: u16) -> io::Result<()> {
        let mut setup: UInputSetup = mem::zeroed();
        setup.id.bustype = BUS_USB;
        setup.id.vendor = vendor;
        setup.id.product = product;
        setup.id.version = 1;

        let bytes = name.as_bytes();
        let len = bytes.len().min(79);
        for (i, &b) in bytes.iter().take(len).enumerate() {
            setup.name[i] = b as libc::c_char;
        }

        Self::ioctl_ptr(fd, UI_DEV_SETUP, &setup as *const _ as *const libc::c_void)?;
        Self::ioctl(fd, UI_DEV_CREATE, 0)?;
        Ok(())
    }

    unsafe fn ioctl(fd: RawFd, req: libc::c_ulong, arg: libc::c_ulong) -> io::Result<()> {
        let res = libc::ioctl(fd, req, arg);
        if res < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(())
        }
    }

    unsafe fn ioctl_ptr(fd: RawFd, req: libc::c_ulong, ptr: *const libc::c_void) -> io::Result<()> {
        let res = libc::ioctl(fd, req, ptr);
        if res < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(())
        }
    }

    pub fn emit(&mut self, type_: u16, code: u16, value: i32) -> io::Result<()> {
        let event = InputEvent {
            time: libc::timeval {
                tv_sec: 0,
                tv_usec: 0,
            },
            type_,
            code,
            value,
        };

        let event_slice = unsafe {
            std::slice::from_raw_parts(
                &event as *const _ as *const u8,
                mem::size_of::<InputEvent>(),
            )
        };

        self.file.write_all(event_slice)?;
        Ok(())
    }

    pub fn sync(&mut self) -> io::Result<()> {
        self.emit(EV_SYN, SYN_REPORT, 0)
    }

    pub fn name(&self) -> &str {
        &self.name
    }
}

impl Drop for UInputDevice {
    fn drop(&mut self) {
        unsafe {
            let _ = libc::ioctl(self.file.as_raw_fd(), UI_DEV_DESTROY);
        }
    }
}

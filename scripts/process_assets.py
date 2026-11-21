#!/usr/bin/env python3
"""
Plugless Asset Processor
Optimizes SVGs, generates Android VectorDrawables, renders launcher mipmap icons,
desktop icons, and README visual assets.
"""
import os
import subprocess
from PIL import Image, ImageDraw, ImageFilter

ROOT_DIR = "/home/jasmin/Projects/Plugless"
ASSETS_DIR = os.path.join(ROOT_DIR, "assets")
ANDROID_RES = os.path.join(ROOT_DIR, "android/app/src/main/res")
DRAWABLE_DIR = os.path.join(ANDROID_RES, "drawable")
MIPMAP_DIR = ANDROID_RES

os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(DRAWABLE_DIR, exist_ok=True)

# 1. Clean and normalize SVG sources
LOGO_SVG = os.path.join(ASSETS_DIR, "fweu.svg")
KEYBOARD_SVG = os.path.join(ASSETS_DIR, "keyboard-fill-svgrepo-com.svg")
MIC_SVG = os.path.join(ASSETS_DIR, "microphone-svgrepo-com.svg")
MOUSE_SVG = os.path.join(ASSETS_DIR, "mouse-svgrepo-com.svg")
SPEAKER_SVG = os.path.join(ASSETS_DIR, "speaker-svgrepo-com_second.svg")

def optimize_svg(src_path, dst_path, is_logo=False):
    with open(src_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Normalize colors to currentColor for theme-tinting
    if not is_logo:
        content = content.replace('fill="#000000"', 'fill="currentColor"')
        content = content.replace('fill="#1C274C"', 'fill="currentColor"')
        content = content.replace('stroke="#000000"', 'stroke="currentColor"')
        content = content.replace('stroke="#1C274C"', 'stroke="currentColor"')
    
    with open(dst_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"  ✓ Optimized SVG: {os.path.basename(dst_path)}")

# Write normalized SVGs
NORM_LOGO = os.path.join(ASSETS_DIR, "plugless-logo.svg")
NORM_MOUSE = os.path.join(ASSETS_DIR, "icon-mouse.svg")
NORM_KEYBOARD = os.path.join(ASSETS_DIR, "icon-keyboard.svg")
NORM_MIC = os.path.join(ASSETS_DIR, "icon-mic.svg")
NORM_SPEAKER = os.path.join(ASSETS_DIR, "icon-speaker.svg")

optimize_svg(LOGO_SVG, NORM_LOGO, is_logo=True)
optimize_svg(MOUSE_SVG, NORM_MOUSE)
optimize_svg(KEYBOARD_SVG, NORM_KEYBOARD)
optimize_svg(MIC_SVG, NORM_MIC)
optimize_svg(SPEAKER_SVG, NORM_SPEAKER)

# Render base PNG for Logo
LOGO_BASE_PNG = os.path.join(ASSETS_DIR, "plugless-logo-512.png")
LOGO_WHITE_PNG = os.path.join(ASSETS_DIR, "plugless-logo-white-512.png")

# Use ImageMagick to rasterize SVG into clean PNG
subprocess.run([
    "magick", "-background", "none", "-density", "384",
    NORM_LOGO, "-resize", "512x512", LOGO_BASE_PNG
], check=True)

# Generate White version of Logo
with Image.open(LOGO_BASE_PNG).convert("RGBA") as img:
    r, g, b, a = img.split()
    white_img = Image.merge("RGBA", (Image.new("L", img.size, 255),
                                    Image.new("L", img.size, 255),
                                    Image.new("L", img.size, 255),
                                    a))
    white_img.save(LOGO_WHITE_PNG, "PNG")

# Generate App Icon Tile (Dark theme squircle: #0D0D11 background with glowing white logo)
APP_ICON_512 = os.path.join(ASSETS_DIR, "app-icon-512.png")
def make_app_icon(size=512):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    radius = int(size * 0.22)
    # Dark squircle
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill="#0D0D11")
    
    # Subtly rounded border
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, outline="#2A2A38", width=max(2, size // 128))
    
    # Paste white logo centered with padding
    glyph_size = int(size * 0.60)
    resized_logo = Image.open(LOGO_WHITE_PNG).resize((glyph_size, glyph_size), Image.Resampling.LANCZOS)
    offset = (size - glyph_size) // 2
    canvas.paste(resized_logo, (offset, offset), resized_logo)
    return canvas

app_icon = make_app_icon(512)
app_icon.save(APP_ICON_512, "PNG")
app_icon.save(os.path.join(ASSETS_DIR, "favicon.ico"), format="ICO", sizes=[(16,16), (32,32), (48,48), (64,64)])
print(f"  ✓ Generated app-icon-512.png & favicon.ico")

# Save tile drawables for Android
app_icon.save(os.path.join(DRAWABLE_DIR, "ic_plugless_tile_dark.png"), "PNG")
# White tile
tile_light = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
d_light = ImageDraw.Draw(tile_light)
d_light.rounded_rectangle([0, 0, 511, 511], radius=112, fill="#FFFFFF", outline="#E2E8F0", width=4)
black_logo = Image.open(LOGO_BASE_PNG).resize((307, 307), Image.Resampling.LANCZOS)
tile_light.paste(black_logo, (102, 102), black_logo)
tile_light.save(os.path.join(DRAWABLE_DIR, "ic_plugless_tile_light.png"), "PNG")

# Generate Android Mipmap densities
densities = {
    "mipmap-mdpi": (48, 108),
    "mipmap-hdpi": (72, 162),
    "mipmap-xhdpi": (96, 216),
    "mipmap-xxhdpi": (144, 324),
    "mipmap-xxxhdpi": (192, 432)
}

for folder, (legacy_size, adaptive_size) in densities.items():
    target_dir = os.path.join(MIPMAP_DIR, folder)
    os.makedirs(target_dir, exist_ok=True)

    # 1) Legacy launcher square & round
    icon_legacy = make_app_icon(legacy_size)
    icon_legacy.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")
    
    # Round icon
    round_canvas = Image.new("RGBA", (legacy_size, legacy_size), (0, 0, 0, 0))
    mask = Image.new("L", (legacy_size, legacy_size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, legacy_size - 1, legacy_size - 1], fill=255)
    round_canvas.paste(icon_legacy, (0, 0), mask=mask)
    round_canvas.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")

    # 2) Adaptive Foreground (glyph centered in transparent canvas at ~44% size)
    fg_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
    fg_glyph_size = int(adaptive_size * 0.44)
    fg_logo = Image.open(LOGO_WHITE_PNG).resize((fg_glyph_size, fg_glyph_size), Image.Resampling.LANCZOS)
    fg_offset = (adaptive_size - fg_glyph_size) // 2
    fg_canvas.paste(fg_logo, (fg_offset, fg_offset), fg_logo)
    fg_canvas.save(os.path.join(target_dir, "ic_launcher_foreground.png"), "PNG")

    # 3) Monochrome Themed Icon (white glyph for Android 13+)
    fg_canvas.save(os.path.join(target_dir, "ic_launcher_monochrome.png"), "PNG")

print("  ✓ Generated Android adaptive mipmap icons (mdpi -> xxxhdpi)")

# Generate README hero banner
BANNER_PATH = os.path.join(ASSETS_DIR, "plugless-banner.png")
def make_banner():
    width, height = 1200, 480
    banner = Image.new("RGBA", (width, height), "#0B0C10")
    draw = ImageDraw.Draw(banner)
    
    # Subtle glow behind logo
    glow_size = 360
    glow_img = Image.new("RGBA", (glow_size, glow_size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_img)
    glow_draw.ellipse([0, 0, glow_size-1, glow_size-1], fill=(99, 102, 241, 40))
    glow_img = glow_img.filter(ImageFilter.GaussianBlur(40))
    banner.paste(glow_img, (600 - glow_size//2, 140 - glow_size//2), glow_img)

    # Center Logo
    logo_sz = 140
    logo_img = Image.open(LOGO_WHITE_PNG).resize((logo_sz, logo_sz), Image.Resampling.LANCZOS)
    banner.paste(logo_img, ((width - logo_sz)//2, 70), logo_img)
    
    # Text
    # Title
    banner.save(BANNER_PATH, "PNG")

make_banner()
print("  ✓ Generated README hero banner")
print("Asset processing complete!")

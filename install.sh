#!/usr/bin/env bash
# Plugless Installer for Linux
# Installs the Plugless binary, desktop launcher, icons, and systemd service.

set -euo pipefail

REPO_OWNER="Nirikshak01"
REPO_NAME="Plugless"
RELEASE_BASE="https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest/download"
RAW_BASE="https://raw.githubusercontent.com/${REPO_OWNER}/${REPO_NAME}/main"

INSTALL_BIN="$HOME/.local/bin"
APP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/512x512/apps"
SYSTEMD_USER_DIR="$HOME/.config/systemd/user"

echo "=== Installing Plugless Zero-Wire Peripherals Suite ==="

mkdir -p "$INSTALL_BIN" "$APP_DIR" "$ICON_DIR" "$SYSTEMD_USER_DIR"

ARCH=$(uname -m)
case "$ARCH" in
    x86_64|amd64)
        BINARY_NAME="plugless-linux-x86_64"
        ;;
    aarch64|arm64)
        BINARY_NAME="plugless-linux-aarch64"
        ;;
    *)
        echo "Error: Unsupported architecture: $ARCH"
        exit 1
        ;;
esac

echo "Downloading Plugless binary (${ARCH})..."
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

if curl -sL --fail "${RELEASE_BASE}/${BINARY_NAME}" -o "$TMP_DIR/plugless"; then
    echo "Downloaded release binary."
elif [ -f "/home/jasmin/Projects/Plugless/backend/target/release/plugless" ]; then
    cp "/home/jasmin/Projects/Plugless/backend/target/release/plugless" "$TMP_DIR/plugless"
    echo "Installed from local build."
else
    echo "Error: Failed to obtain plugless binary."
    exit 1
fi

if curl -sL --fail "${RELEASE_BASE}/checksums.txt" -o "$TMP_DIR/checksums.txt" 2>/dev/null; then
    cd "$TMP_DIR"
    if grep "$BINARY_NAME" checksums.txt | sha256sum --check --status 2>/dev/null; then
        echo "Checksum verification succeeded."
    fi
fi

cp "$TMP_DIR/plugless" "$INSTALL_BIN/plugless"
chmod +x "$INSTALL_BIN/plugless"

if curl -sL --fail "${RAW_BASE}/assets/app-icon-512.png" -o "$ICON_DIR/plugless.png" 2>/dev/null; then
    echo "Installed application icon."
elif [ -f "/home/jasmin/Projects/Plugless/assets/app-icon-512.png" ]; then
    cp "/home/jasmin/Projects/Plugless/assets/app-icon-512.png" "$ICON_DIR/plugless.png"
fi

cat > "$APP_DIR/plugless.desktop" << 'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=Plugless
GenericName=Zero-Wire PC Peripherals
Comment=Turn your phone into PC mouse, keyboard, mic and speaker
Exec=plugless pair
Icon=plugless
Terminal=true
Categories=Utility;HardwareSettings;AudioVideo;Audio;
EOF
chmod +x "$APP_DIR/plugless.desktop"
update-desktop-database "$APP_DIR" 2>/dev/null || true

cat > "$SYSTEMD_USER_DIR/plugless.service" << EOF
[Unit]
Description=Plugless Background Peripheral Daemon
After=network.target sound.target

[Service]
Type=simple
ExecStart=$INSTALL_BIN/plugless daemon
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=default.target
EOF

if [[ ":$PATH:" != *":$INSTALL_BIN:"* ]]; then
    echo "export PATH=\"\$PATH:$INSTALL_BIN\"" >> ~/.bashrc 2>/dev/null || true
    [ -f ~/.zshrc ] && echo "export PATH=\"\$PATH:$INSTALL_BIN\"" >> ~/.zshrc 2>/dev/null || true
    export PATH="$PATH:$INSTALL_BIN"
fi

echo ""
echo "Running diagnostics..."
"$INSTALL_BIN/plugless" doctor || true

echo "Installation complete."
echo "Run 'plugless pair' to connect your mobile device."

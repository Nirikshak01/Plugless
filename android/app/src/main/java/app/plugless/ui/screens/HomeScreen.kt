package app.plugless.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plugless.R
import app.plugless.model.ConnectionState
import app.plugless.model.ToolType
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*

@Composable
fun HomeScreen(
    service: PluglessService?,
    onNavigateTouchpad: () -> Unit,
    onNavigateKeyboard: () -> Unit,
    onNavigateScanner: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val connectionState = service?.connectionState ?: ConnectionState.DISCONNECTED
    val latencyMs = service?.latencyMs ?: 0L
    val mouseOn = service?.mouseEnabled ?: false
    val kbOn = service?.keyboardEnabled ?: false
    val micOn = service?.micEnabled ?: false
    val spkOn = service?.speakerEnabled ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo Tile
                Image(
                    painter = painterResource(id = R.drawable.ic_plugless_tile_dark),
                    contentDescription = "Plugless Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Column {
                    Text(
                        text = "Plugless",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zero-Wire Peripherals",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection Pill
                ConnectionStatusPill(
                    connectionState = connectionState,
                    latencyMs = latencyMs,
                    onTap = {
                        if (connectionState == ConnectionState.DISCONNECTED) {
                            onNavigateScanner()
                        } else {
                            service?.disconnect()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Settings Gear
                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(CardBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- PRESET ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetPill(
                label = "📞 Meeting",
                isActive = micOn && spkOn,
                onClick = { service?.applyPreset("meeting") }
            )
            PresetPill(
                label = "🎮 Remote",
                isActive = mouseOn && kbOn,
                onClick = { service?.applyPreset("remote") }
            )
            PresetPill(
                label = "⚡ All On",
                isActive = mouseOn && kbOn && micOn && spkOn,
                onClick = { service?.applyPreset("all") }
            )
        }

        // --- 2x2 PERIPHERAL TILES ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Mouse & Keyboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PeripheralTile(
                    title = "Mouse",
                    subtitle = if (mouseOn) "Touchpad active" else "Tap to toggle",
                    iconRes = R.drawable.ic_mouse,
                    isActive = mouseOn,
                    modifier = Modifier.weight(1f),
                    onToggle = { service?.toggleTool(ToolType.MOUSE) },
                    onOpenScreen = onNavigateTouchpad
                )

                PeripheralTile(
                    title = "Keyboard",
                    subtitle = if (kbOn) "Typing & hotkeys" else "Tap to toggle",
                    iconRes = R.drawable.ic_keyboard,
                    isActive = kbOn,
                    modifier = Modifier.weight(1f),
                    onToggle = { service?.toggleTool(ToolType.KEYBOARD) },
                    onOpenScreen = onNavigateKeyboard
                )
            }

            // Row 2: Microphone & Speaker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PeripheralTile(
                    title = "Microphone",
                    subtitle = if (micOn) "Streaming to PC" else "Virtual mic off",
                    iconRes = R.drawable.ic_mic,
                    isActive = micOn,
                    modifier = Modifier.weight(1f),
                    onToggle = { service?.toggleTool(ToolType.MIC) },
                    onOpenScreen = null
                )

                PeripheralTile(
                    title = "Speaker",
                    subtitle = if (spkOn) "Playing PC audio" else "Speaker off",
                    iconRes = R.drawable.ic_speaker,
                    isActive = spkOn,
                    modifier = Modifier.weight(1f),
                    onToggle = { service?.toggleTool(ToolType.SPEAKER) },
                    onOpenScreen = null
                )
            }
        }

        // --- BOTTOM ACTION (SCAN / STATUS) ---
        if (connectionState == ConnectionState.DISCONNECTED) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onNavigateScanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = AccentIndigo),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentIndigo
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan PC",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Scan PC Terminal QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusPill(
    connectionState: ConnectionState,
    latencyMs: Long,
    onTap: () -> Unit
) {
    val (bgColor, dotColor, text) = when (connectionState) {
        ConnectionState.CONNECTED -> Triple(Color(0xFF0F291E), SuccessGreen, "$latencyMs ms")
        ConnectionState.CONNECTING -> Triple(Color(0xFF2D2305), WarningAmber, "Connecting")
        ConnectionState.DISCONNECTED -> Triple(CardBackground, TextSecondary, "Scan PC")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PresetPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) AccentIndigo.copy(alpha = 0.25f) else CardBackground)
            .border(
                width = 1.dp,
                color = if (isActive) AccentIndigo else CardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun PeripheralTile(
    title: String,
    subtitle: String,
    iconRes: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onOpenScreen: (() -> Unit)?
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) CardBorderActive else CardBorder,
        animationSpec = tween(300),
        label = "tileBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF131524) else CardBackground,
        animationSpec = tween(300),
        label = "tileBg"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable {
                if (onOpenScreen != null && isActive) {
                    onOpenScreen()
                } else {
                    onToggle()
                }
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Icon and Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) AccentIndigo.copy(alpha = 0.2f) else Color(0xFF1D202D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = if (isActive) AccentIndigo else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentIndigo,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF1C1F2C),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            // Text section
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) AccentCyan else TextSecondary
                )
            }

            // Quick Controller indicator if separate screen exists
            if (onOpenScreen != null && isActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onOpenScreen() }
                ) {
                    Text(
                        text = "Open Controller",
                        fontSize = 11.sp,
                        color = AccentIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open",
                        tint = AccentIndigo,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size((52 * scale).dp, (32 * scale).dp)
)

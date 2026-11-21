package app.plugless.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plugless.R
import app.plugless.model.AppSettings
import app.plugless.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(CardBackground, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }

        // --- LOGO & HEADER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_plugless_tile_dark),
                contentDescription = "Plugless Tile",
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Plugless v1.0.0",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Zero-Wire PC Peripherals",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- THEME SELECTOR ---
        SettingsSectionHeader("APPEARANCE")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeOption("system", "System", settings.themeMode, Modifier.weight(1f)) {
                onSettingsChanged(settings.copy(themeMode = it))
            }
            ThemeOption("light", "Light", settings.themeMode, Modifier.weight(1f)) {
                onSettingsChanged(settings.copy(themeMode = it))
            }
            ThemeOption("dark", "Dark", settings.themeMode, Modifier.weight(1f)) {
                onSettingsChanged(settings.copy(themeMode = it))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- MOUSE & TRACKPAD ---
        SettingsSectionHeader("MOUSE & TRACKPAD")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pointer Sensitivity: ${String.format("%.1f", settings.mouseSensitivity)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = settings.mouseSensitivity,
                    onValueChange = { onSettingsChanged(settings.copy(mouseSensitivity = it)) },
                    valueRange = 0.5f..3.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentIndigo,
                        activeTrackColor = AccentIndigo,
                        inactiveTrackColor = CardBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsToggleRow(
                    title = "Natural Scrolling",
                    subtitle = "Scroll contents in direction of finger swipe",
                    checked = settings.naturalScroll,
                    onCheckedChange = { onSettingsChanged(settings.copy(naturalScroll = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- MICROPHONE & SPEECH ---
        SettingsSectionHeader("AUDIO & COMBINATION MODE")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    title = "AI Noise Suppression",
                    subtitle = "Filter out background keyboard clicks and ambient noise",
                    checked = settings.noiseSuppression,
                    onCheckedChange = { onSettingsChanged(settings.copy(noiseSuppression = it)) }
                )

                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingsToggleRow(
                    title = "Acoustic Echo Cancellation",
                    subtitle = "Prevent feedback loop when mic and speaker are both on",
                    checked = settings.echoCancellation,
                    onCheckedChange = { onSettingsChanged(settings.copy(echoCancellation = it)) }
                )

                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Microphone Gain Boost: ${String.format("%.1f", settings.micGain)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = settings.micGain,
                    onValueChange = { onSettingsChanged(settings.copy(micGain = it)) },
                    valueRange = 0.5f..2.5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = CardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- HARDWARE & SYSTEM ---
        SettingsSectionHeader("SYSTEM PREFERENCES")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent display sleep while trackpad is open",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onSettingsChanged(settings.copy(keepScreenOn = it)) }
                )

                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingsToggleRow(
                    title = "Haptic Vibration Feedback",
                    subtitle = "Vibrate softly on touch clicks and QR scan",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { onSettingsChanged(settings.copy(hapticFeedback = it)) }
                )

                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingsToggleRow(
                    title = "Auto-Connect on Startup",
                    subtitle = "Automatically reconnect to last paired PC",
                    checked = settings.autoConnect,
                    onCheckedChange = { onSettingsChanged(settings.copy(autoConnect = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- LINKS & GITHUB ---
        SettingsSectionHeader("ABOUT & COMMUNITY")
        SettingsCard {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsLinkRow(
                    icon = Icons.Default.Code,
                    title = "GitHub Repository",
                    subtitle = "https://github.com/Nirikshak01/Plugless",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nirikshak01/Plugless"))
                        context.startActivity(intent)
                    }
                )

                SettingsLinkRow(
                    icon = Icons.Default.Security,
                    title = "Privacy & Zero Telemetry",
                    subtitle = "100% LAN local, no cloud servers or tracking",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
fun ThemeOption(
    id: String,
    label: String,
    selected: String,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == id
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AccentIndigo else Color.Transparent)
            .clickable { onSelect(id) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentIndigo,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF1D202D),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AccentIndigo,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

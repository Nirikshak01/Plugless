package app.plugless.ui.screens

import android.content.Intent
import android.net.Uri
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
            .background(WhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(CardGreyLight, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        // --- APP HEADER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyLight)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plugless_logo),
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        text = "Plugless",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Version 1.0.0 • Zero-Wire PC Peripherals",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // --- 1. CONNECTION & PAIRING ---
        SettingsSectionHeader("Connection")
        SettingsCard {
            SettingsToggleRow(
                title = "Auto-Reconnect to PC",
                subtitle = "Automatically re-pair with last known PC on app launch",
                checked = settings.autoConnect,
                onCheckedChange = { onSettingsChanged(settings.copy(autoConnect = it)) }
            )

            if (settings.lastHostIp != null) {
                HorizontalDivider(color = CardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Paired PC IP", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text(settings.lastHostIp ?: "None", fontSize = 12.sp, color = TextMuted)
                    }

                    TextButton(
                        onClick = {
                            onSettingsChanged(settings.copy(lastHostIp = null, lastToken = null))
                        }
                    ) {
                        Text("Forget", color = StatusRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // --- 2. MOUSE & TOUCHPAD ---
        SettingsSectionHeader("Mouse & Touchpad")
        SettingsCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mouse Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    Text("${String.format("%.1f", settings.mouseSensitivity)}x", fontSize = 13.sp, color = TextMuted)
                }
                Slider(
                    value = settings.mouseSensitivity,
                    onValueChange = { onSettingsChanged(settings.copy(mouseSensitivity = it)) },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandBlack,
                        activeTrackColor = BrandBlack,
                        inactiveTrackColor = CardBorder
                    )
                )
            }

            HorizontalDivider(color = CardBorder)

            SettingsToggleRow(
                title = "Natural Scrolling",
                subtitle = "Content moves in direction of two-finger swipe",
                checked = settings.naturalScroll,
                onCheckedChange = { onSettingsChanged(settings.copy(naturalScroll = it)) }
            )
        }

        // --- 3. SYSTEM & HARDWARE ---
        SettingsSectionHeader("System & Display")
        SettingsCard {
            SettingsToggleRow(
                title = "Keep Screen On",
                subtitle = "Prevents screen sleep while active or connected",
                checked = settings.keepScreenOn,
                onCheckedChange = { onSettingsChanged(settings.copy(keepScreenOn = it)) }
            )

            HorizontalDivider(color = CardBorder)

            SettingsToggleRow(
                title = "Haptic Feedback",
                subtitle = "Vibrate on button clicks and QR detection",
                checked = settings.hapticFeedback,
                onCheckedChange = { onSettingsChanged(settings.copy(hapticFeedback = it)) }
            )
        }

        // --- 4. ABOUT & GITHUB ---
        SettingsSectionHeader("About")
        SettingsCard {
            SettingsLinkRow(
                title = "GitHub Repository",
                subtitle = "github.com/Nirikshak01/Plugless",
                icon = Icons.Default.Code,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nirikshak01/Plugless"))
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(color = CardBorder)

            SettingsLinkRow(
                title = "Download Latest APK",
                subtitle = "Check for updates and release notes",
                icon = Icons.Default.Download,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nirikshak01/Plugless/releases"))
                    context.startActivity(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreyLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandBlack,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextDark, modifier = Modifier.size(20.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
        }

        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

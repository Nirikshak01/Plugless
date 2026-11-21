package app.plugless.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plugless.R
import app.plugless.model.ToolType
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*

@Composable
fun MicrophoneScreen(
    service: PluglessService?,
    onBack: () -> Unit
) {
    val micActive = service?.micEnabled ?: false
    var noiseSuppression by remember { mutableStateOf(true) }
    var echoCancellation by remember { mutableStateOf(true) }
    var micGain by remember { mutableFloatStateOf(1.0f) }

    // Pulsing audio animation
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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

            Text(
                text = "Microphone",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            // Prominent ON / OFF Switch
            Switch(
                checked = micActive,
                onCheckedChange = { isChecked ->
                    service?.setToolEnabled(ToolType.MIC, isChecked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CardBorder
                )
            )
        }

        // --- CENTER VISUALIZER & STATUS ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(if (micActive) CardActive else CardGreyLight)
                    .border(
                        width = if (micActive) 2.dp else 1.dp,
                        color = if (micActive) BorderActive else CardBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (micActive) {
                    Box(
                        modifier = Modifier
                            .size((120 * waveScale).dp)
                            .clip(CircleShape)
                            .background(Color(0x3310B981))
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_mic),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (micActive) BorderActive else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (micActive) "● Microphone Active" else "Microphone Stopped",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (micActive) BorderActive else TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (micActive) {
                    "Streaming phone mic to PC as 'Plugless_Virtual_Mic'"
                } else {
                    "Turn switch ON to stream phone mic to Zoom, Discord & OBS"
                },
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 18.sp
            )
        }

        // --- SETTINGS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyLight)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mic Gain Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Microphone Gain", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("${(micGain * 100).toInt()}%", fontSize = 14.sp, color = TextMuted)
                    }
                    Slider(
                        value = micGain,
                        onValueChange = { micGain = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandBlack,
                            activeTrackColor = BrandBlack,
                            inactiveTrackColor = CardBorder
                        )
                    )
                }

                HorizontalDivider(color = CardBorder)

                // Noise Suppression Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Noise Suppression", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("Reduces background fans & typing noise", fontSize = 12.sp, color = TextMuted)
                    }
                    Switch(
                        checked = noiseSuppression,
                        onCheckedChange = { noiseSuppression = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandBlack
                        )
                    )
                }

                HorizontalDivider(color = CardBorder)

                // Echo Cancellation Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Acoustic Echo Cancellation", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("Prevents speaker feedback during calls", fontSize = 12.sp, color = TextMuted)
                    }
                    Switch(
                        checked = echoCancellation,
                        onCheckedChange = { echoCancellation = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandBlack
                        )
                    )
                }
            }
        }
    }
}

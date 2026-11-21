package app.plugless.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SpeakerScreen(
    service: PluglessService?,
    onBack: () -> Unit
) {
    val speakerActive by (service?.speakerEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    var volume by remember { mutableFloatStateOf(1.0f) }
    var latencyMode by remember { mutableStateOf("Low Latency") } // "Low Latency", "Balanced", "Stable"

    // Wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "speakerPulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakerScale"
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
                text = "Speaker",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            // Prominent ON / OFF Switch
            Switch(
                checked = speakerActive,
                onCheckedChange = { isChecked ->
                    service?.setToolEnabled(ToolType.SPEAKER, isChecked)
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
                    .background(if (speakerActive) CardActive else CardGreyLight)
                    .border(
                        width = if (speakerActive) 2.dp else 1.dp,
                        color = if (speakerActive) BorderActive else CardBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (speakerActive) {
                    Box(
                        modifier = Modifier
                            .size((120 * waveScale).dp)
                            .clip(CircleShape)
                            .background(Color(0x3310B981))
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_speaker),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (speakerActive) BorderActive else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (speakerActive) "● Playing PC Sound" else "Speaker Stopped",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (speakerActive) BorderActive else TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (speakerActive) {
                    "Listening to PC system audio (@DEFAULT_MONITOR@)"
                } else {
                    "Turn switch ON to route all PC audio to phone speakers or earphones"
                },
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 18.sp
            )
        }

        // --- CONTROLS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyLight)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Speaker Volume Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Playback Volume", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("${(volume * 100).toInt()}%", fontSize = 14.sp, color = TextMuted)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0.0f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandBlack,
                            activeTrackColor = BrandBlack,
                            inactiveTrackColor = CardBorder
                        )
                    )
                }

                HorizontalDivider(color = CardBorder)

                // Latency Mode Selector
                Column {
                    Text("Jitter Buffer Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low Latency", "Balanced", "Stable").forEach { mode ->
                            val isSelected = latencyMode == mode
                            Button(
                                onClick = { latencyMode = mode },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) BrandBlack else WhiteSurface,
                                    contentColor = if (isSelected) Color.White else TextDark
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

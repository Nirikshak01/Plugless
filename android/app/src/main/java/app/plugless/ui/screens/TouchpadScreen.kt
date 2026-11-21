package app.plugless.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plugless.model.AppSettings
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadScreen(
    service: PluglessService?,
    settings: AppSettings,
    onBack: () -> Unit
) {
    var sensitivity by remember { mutableFloatStateOf(settings.mouseSensitivity) }
    var lastX by remember { mutableFloatStateOf(0f) }
    var lastY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableBooleanOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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

            Text(
                text = "Touchpad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Sensitivity indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Sensitivity",
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${String.format("%.1f", sensitivity)}x",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- TRACKPAD AREA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastX = motionEvent.x
                            lastY = motionEvent.y
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val count = motionEvent.pointerCount
                            if (count == 1) {
                                // 1-Finger Move
                                val dx = (motionEvent.x - lastX) * sensitivity
                                val dy = (motionEvent.y - lastY) * sensitivity
                                service?.client?.sendMouseMove(dx, dy)
                                lastX = motionEvent.x
                                lastY = motionEvent.y
                            } else if (count == 2) {
                                // 2-Finger Scroll
                                val dy = (motionEvent.y - lastY) * settings.scrollSpeed * 0.1f
                                val dx = (motionEvent.x - lastX) * settings.scrollSpeed * 0.1f
                                val scrollY = if (settings.naturalScroll) dy else -dy
                                service?.client?.sendMouseScroll(dx, scrollY)
                                lastX = motionEvent.x
                                lastY = motionEvent.y
                            }
                            true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // 2-Finger Tap -> Right click
                            if (motionEvent.pointerCount == 2) {
                                service?.client?.sendMouseClick("right", "click")
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val moveDist = Math.hypot(
                                (motionEvent.x - lastX).toDouble(),
                                (motionEvent.y - lastY).toDouble()
                            )
                            if (moveDist < 8.0 && motionEvent.eventTime - motionEvent.downTime < 200) {
                                // Tap = Left Click
                                service?.client?.sendMouseClick("left", "click")
                            }
                            if (isDragging) {
                                isDragging = false
                                service?.client?.sendMouseClick("left", "up")
                            }
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Trackpad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary.copy(alpha = 0.5f)
                )
                Text(
                    text = "1-Finger Move & Tap • 2-Finger Scroll & Right Click",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- BOTTOM MOUSE BUTTONS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Click Button
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1B1D2A))
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable { service?.client?.sendMouseClick("left", "click") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEFT CLICK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }

            // Middle Click Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1B1D2A))
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable { service?.client?.sendMouseClick("middle", "click") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MIDDLE",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Right Click Button
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1B1D2A))
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable { service?.client?.sendMouseClick("right", "click") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RIGHT CLICK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

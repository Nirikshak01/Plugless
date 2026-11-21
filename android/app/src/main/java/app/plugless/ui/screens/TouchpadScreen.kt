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
import app.plugless.model.ToolType
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadScreen(
    service: PluglessService?,
    settings: AppSettings,
    onBack: () -> Unit
) {
    val mouseActive = service?.mouseEnabled ?: false
    var sensitivity by remember { mutableFloatStateOf(settings.mouseSensitivity) }
    var lastX by remember { mutableFloatStateOf(0f) }
    var lastY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        // --- TOP BAR ---
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
                    .background(CardGreyLight, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }

            Text(
                text = "Touchpad & Mouse",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            // Prominent ON / OFF Switch
            Switch(
                checked = mouseActive,
                onCheckedChange = { isChecked ->
                    service?.setToolEnabled(ToolType.MOUSE, isChecked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CardBorder
                )
            )
        }

        // --- SENSITIVITY BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Speed", fontSize = 12.sp, color = TextMuted)
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.5f..3.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = BrandBlack,
                    activeTrackColor = BrandBlack,
                    inactiveTrackColor = CardBorder
                )
            )
            Text("${String.format("%.1f", sensitivity)}x", fontSize = 12.sp, color = TextMuted)
        }

        // --- MAIN TOUCHPAD SURFACE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (mouseActive) CardGrey else CardGreyLight)
                .border(
                    width = 2.dp,
                    color = if (mouseActive) BorderActive else CardBorder,
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInteropFilter { event ->
                    if (!mouseActive) return@pointerInteropFilter true
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastX = event.x
                            lastY = event.y
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val pointerCount = event.pointerCount
                            if (pointerCount == 1) {
                                val dx = (event.x - lastX) * sensitivity
                                val dy = (event.y - lastY) * sensitivity
                                service?.client?.sendMouseMove(dx, dy)
                                lastX = event.x
                                lastY = event.y
                            } else if (pointerCount == 2) {
                                val dy = (event.y - lastY) * sensitivity * 0.5f
                                service?.client?.sendMouseScroll(0f, dy)
                                lastY = event.y
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (mouseActive) BrandBlack else TextMuted
                )
                Text(
                    text = if (mouseActive) "Touchpad Active" else "Touchpad Stopped",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (mouseActive) TextDark else TextMuted
                )
                Text(
                    text = "1 finger: move • tap: click • 2 fingers: scroll",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- MOUSE BUTTONS ROW (Left, Drag, Right) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Click
            Button(
                onClick = { service?.client?.sendMouseClick("left", "click") },
                enabled = mouseActive,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlack)
            ) {
                Text("Left Click", fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Drag Lock Toggle
            Button(
                onClick = {
                    isDragging = !isDragging
                    val state = if (isDragging) "down" else "up"
                    service?.client?.sendMouseClick("left", state)
                },
                enabled = mouseActive,
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDragging) StatusGreen else CardGreyLight
                )
            ) {
                Text(
                    text = if (isDragging) "Dragging" else "Drag",
                    fontWeight = FontWeight.Medium,
                    color = if (isDragging) Color.White else TextDark
                )
            }

            // Right Click
            Button(
                onClick = { service?.client?.sendMouseClick("right", "click") },
                enabled = mouseActive,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlack)
            ) {
                Text("Right Click", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

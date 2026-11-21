package app.plugless.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plugless.model.ToolType
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun KeyboardScreen(
    service: PluglessService?,
    onBack: () -> Unit
) {
    val keyboardActive by (service?.keyboardEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    var inputText by remember { mutableStateOf("") }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var superActive by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (ctrlActive) service?.client?.sendSpecialKey("ctrl", "up")
            if (altActive) service?.client?.sendSpecialKey("alt", "up")
            if (shiftActive) service?.client?.sendSpecialKey("shift", "up")
            if (superActive) service?.client?.sendSpecialKey("super", "up")
        }
    }

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
                text = "PC Keyboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            // Prominent ON / OFF Switch
            Switch(
                checked = keyboardActive,
                onCheckedChange = { isChecked ->
                    if (!isChecked) {
                        if (ctrlActive) { ctrlActive = false; service?.client?.sendSpecialKey("ctrl", "up") }
                        if (altActive) { altActive = false; service?.client?.sendSpecialKey("alt", "up") }
                        if (shiftActive) { shiftActive = false; service?.client?.sendSpecialKey("shift", "up") }
                        if (superActive) { superActive = false; service?.client?.sendSpecialKey("super", "up") }
                    }
                    service?.setToolEnabled(ToolType.KEYBOARD, isChecked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CardBorder
                )
            )
        }

        // --- DIRECT TYPING INPUT BOX ---
        OutlinedTextField(
            value = inputText,
            onValueChange = { newText ->
                if (keyboardActive && newText.length > inputText.length) {
                    val typed = newText.substring(inputText.length)
                    service?.client?.sendText(typed)
                }
                inputText = newText
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Type here to send keystrokes directly to PC...") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (keyboardActive) {
                        service?.client?.sendSpecialKey("enter", "click")
                        inputText = ""
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlack,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = WhiteSurface,
                unfocusedContainerColor = WhiteSurface
            )
        )

        // --- SPECIAL MODIFIERS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModifierKeyButton("Ctrl", ctrlActive, Modifier.weight(1f)) {
                ctrlActive = !ctrlActive
                val state = if (ctrlActive) "down" else "up"
                service?.client?.sendSpecialKey("ctrl", state)
            }
            ModifierKeyButton("Alt", altActive, Modifier.weight(1f)) {
                altActive = !altActive
                val state = if (altActive) "down" else "up"
                service?.client?.sendSpecialKey("alt", state)
            }
            ModifierKeyButton("Shift", shiftActive, Modifier.weight(1f)) {
                shiftActive = !shiftActive
                val state = if (shiftActive) "down" else "up"
                service?.client?.sendSpecialKey("shift", state)
            }
            ModifierKeyButton("Super", superActive, Modifier.weight(1f)) {
                superActive = !superActive
                val state = if (superActive) "down" else "up"
                service?.client?.sendSpecialKey("super", state)
            }
        }

        // --- FUNCTION KEYS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionKeyButton("Esc", Modifier.weight(1f)) {
                service?.client?.sendSpecialKey("esc", "click")
            }
            ActionKeyButton("Tab", Modifier.weight(1f)) {
                service?.client?.sendSpecialKey("tab", "click")
            }
            ActionKeyButton("Bksp", Modifier.weight(1f)) {
                service?.client?.sendSpecialKey("backspace", "click")
            }
            ActionKeyButton("Enter", Modifier.weight(1f)) {
                service?.client?.sendSpecialKey("enter", "click")
            }
        }

        // --- SHORTCUTS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quick PC Shortcuts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                // Row of quick combos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutPill("Ctrl+C (Copy)", Modifier.weight(1f)) {
                        service?.client?.sendSpecialKey("ctrl", "down")
                        service?.client?.sendText("c")
                        service?.client?.sendSpecialKey("ctrl", "up")
                    }
                    ShortcutPill("Ctrl+V (Paste)", Modifier.weight(1f)) {
                        service?.client?.sendSpecialKey("ctrl", "down")
                        service?.client?.sendText("v")
                        service?.client?.sendSpecialKey("ctrl", "up")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutPill("Alt+Tab (Switch)", Modifier.weight(1f)) {
                        service?.client?.sendSpecialKey("alt", "down")
                        service?.client?.sendSpecialKey("tab", "click")
                        service?.client?.sendSpecialKey("alt", "up")
                    }
                    ShortcutPill("Super (App Menu)", Modifier.weight(1f)) {
                        service?.client?.sendSpecialKey("super", "click")
                    }
                }

                // Arrow Navigation Pad
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { service?.client?.sendSpecialKey("up", "click") },
                        modifier = Modifier.size(44.dp).background(WhiteSurface, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextDark)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IconButton(
                            onClick = { service?.client?.sendSpecialKey("left", "click") },
                            modifier = Modifier.size(44.dp).background(WhiteSurface, CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextDark)
                        }
                        IconButton(
                            onClick = { service?.client?.sendSpecialKey("down", "click") },
                            modifier = Modifier.size(44.dp).background(WhiteSurface, CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextDark)
                        }
                        IconButton(
                            onClick = { service?.client?.sendSpecialKey("right", "click") },
                            modifier = Modifier.size(44.dp).background(WhiteSurface, CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierKeyButton(
    title: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) BrandBlack else WhiteSurface
        ),
        border = BorderStroke(1.dp, if (isActive) BrandBlack else CardBorder),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) Color.White else TextDark
        )
    }
}

@Composable
private fun ActionKeyButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WhiteSurface),
        border = BorderStroke(1.dp, CardBorder),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark
        )
    }
}

@Composable
private fun ShortcutPill(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = WhiteSurface,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
        }
    }
}

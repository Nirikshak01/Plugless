package app.plugless.ui.screens

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
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*

@Composable
fun KeyboardScreen(
    service: PluglessService?,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var superActive by remember { mutableStateOf(false) }

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
                text = "PC Remote Keyboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.size(44.dp))
        }

        // --- TYPING INPUT FIELD ---
        OutlinedTextField(
            value = inputText,
            onValueChange = { newText ->
                if (newText.length > inputText.length) {
                    val added = newText.substring(inputText.length)
                    service?.client?.sendText(added)
                } else if (newText.length < inputText.length) {
                    service?.client?.sendSpecialKey("backspace", "click")
                }
                inputText = newText
            },
            placeholder = { Text("Tap here to type on PC...", color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    service?.client?.sendSpecialKey("enter", "click")
                    inputText = ""
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = AccentIndigo,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- MODIFIER KEYS ROW (Ctrl, Alt, Shift, Super, Esc, Tab) ---
        Text(
            text = "MODIFIER KEYS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModifierKeyChip(
                name = "Ctrl",
                isActive = ctrlActive,
                modifier = Modifier.weight(1f),
                onClick = {
                    ctrlActive = !ctrlActive
                    service?.client?.sendSpecialKey("ctrl", if (ctrlActive) "down" else "up")
                }
            )
            ModifierKeyChip(
                name = "Alt",
                isActive = altActive,
                modifier = Modifier.weight(1f),
                onClick = {
                    altActive = !altActive
                    service?.client?.sendSpecialKey("alt", if (altActive) "down" else "up")
                }
            )
            ModifierKeyChip(
                name = "Shift",
                isActive = shiftActive,
                modifier = Modifier.weight(1f),
                onClick = {
                    shiftActive = !shiftActive
                    service?.client?.sendSpecialKey("shift", if (shiftActive) "down" else "up")
                }
            )
            ModifierKeyChip(
                name = "Super",
                isActive = superActive,
                modifier = Modifier.weight(1f),
                onClick = {
                    superActive = !superActive
                    service?.client?.sendSpecialKey("super", if (superActive) "down" else "up")
                }
            )
            KeyButton(
                label = "Esc",
                modifier = Modifier.weight(1f),
                onClick = { service?.client?.sendSpecialKey("esc", "click") }
            )
            KeyButton(
                label = "Tab",
                modifier = Modifier.weight(1f),
                onClick = { service?.client?.sendSpecialKey("tab", "click") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- QUICK SHORTCUTS ---
        Text(
            text = "QUICK SHORTCUTS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyButton(
                label = "Copy",
                modifier = Modifier.weight(1f),
                onClick = {
                    service?.client?.sendSpecialKey("ctrl", "down")
                    service?.client?.sendText("c")
                    service?.client?.sendSpecialKey("ctrl", "up")
                }
            )
            KeyButton(
                label = "Paste",
                modifier = Modifier.weight(1f),
                onClick = {
                    service?.client?.sendSpecialKey("ctrl", "down")
                    service?.client?.sendText("v")
                    service?.client?.sendSpecialKey("ctrl", "up")
                }
            )
            KeyButton(
                label = "Undo",
                modifier = Modifier.weight(1f),
                onClick = {
                    service?.client?.sendSpecialKey("ctrl", "down")
                    service?.client?.sendText("z")
                    service?.client?.sendSpecialKey("ctrl", "up")
                }
            )
            KeyButton(
                label = "Alt+Tab",
                modifier = Modifier.weight(1.2f),
                onClick = {
                    service?.client?.sendSpecialKey("alt", "down")
                    service?.client?.sendSpecialKey("tab", "click")
                    service?.client?.sendSpecialKey("alt", "up")
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ARROW KEYS NAVIGATION PAD ---
        Text(
            text = "NAVIGATION PAD",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Up
            KeyButton(
                label = "▲",
                modifier = Modifier.size(68.dp, 54.dp),
                onClick = { service?.client?.sendSpecialKey("up", "click") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Left, Down, Right
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton(
                    label = "◀",
                    modifier = Modifier.size(68.dp, 54.dp),
                    onClick = { service?.client?.sendSpecialKey("left", "click") }
                )
                KeyButton(
                    label = "▼",
                    modifier = Modifier.size(68.dp, 54.dp),
                    onClick = { service?.client?.sendSpecialKey("down", "click") }
                )
                KeyButton(
                    label = "▶",
                    modifier = Modifier.size(68.dp, 54.dp),
                    onClick = { service?.client?.sendSpecialKey("right", "click") }
                )
            }
        }
    }
}

@Composable
fun ModifierKeyChip(
    name: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) AccentIndigo else CardBackground)
            .border(
                1.dp,
                if (isActive) Color.White else CardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else TextPrimary
        )
    }
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

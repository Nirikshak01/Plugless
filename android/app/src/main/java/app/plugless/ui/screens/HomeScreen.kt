package app.plugless.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plugless.R
import app.plugless.model.ConnectionState
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun HomeScreen(
    service: PluglessService?,
    onNavigateTouchpad: () -> Unit,
    onNavigateKeyboard: () -> Unit,
    onNavigateMicrophone: () -> Unit,
    onNavigateSpeaker: () -> Unit,
    onNavigateCamera: () -> Unit,
    onNavigateScanner: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val connectionState by (service?.connectionState ?: remember { MutableStateFlow(ConnectionState.DISCONNECTED) }).collectAsState()
    val mouseOn by (service?.mouseEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    val kbOn by (service?.keyboardEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    val micOn by (service?.micEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    val spkOn by (service?.speakerEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    val camOn by (service?.cameraEnabled ?: remember { MutableStateFlow(false) }).collectAsState()

    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showRequestToolDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Black rounded square logo box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plugless_logo),
                    contentDescription = "Plugless Logo",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Right: Settings Outline Gear Icon
            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = TextDark,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // --- HERO ILLUSTRATION CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.banner_scan_illustration),
                    contentDescription = "Scan PC Peripherals Illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- ACTION PILL BUTTON (Scan / Connected) ---
        Surface(
            onClick = {
                if (connectionState == ConnectionState.CONNECTED) {
                    showDisconnectDialog = true
                } else {
                    onNavigateScanner()
                }
            },
            shape = RoundedCornerShape(50.dp),
            color = if (connectionState == ConnectionState.CONNECTED) ConnectedPillBg else PillButtonBg,
            modifier = Modifier
                .width(160.dp)
                .height(46.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = when (connectionState) {
                        ConnectionState.CONNECTED -> "● Connected"
                        ConnectionState.CONNECTING -> "Connecting..."
                        ConnectionState.DISCONNECTED -> "Scan"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (connectionState == ConnectionState.CONNECTED) ConnectedPillText else PillButtonText
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- 2x2 PERIPHERAL TILES GRID ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mouse Tile
            PeripheralTile(
                title = "Mouse",
                iconResId = R.drawable.ic_mouse,
                isActive = mouseOn,
                modifier = Modifier.weight(1f),
                onClick = onNavigateTouchpad
            )

            // Keyboard Tile
            PeripheralTile(
                title = "Keyboard",
                iconResId = R.drawable.ic_keyboard,
                isActive = kbOn,
                modifier = Modifier.weight(1f),
                onClick = onNavigateKeyboard
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Microphone Tile
            PeripheralTile(
                title = "Microphone",
                iconResId = R.drawable.ic_mic,
                isActive = micOn,
                modifier = Modifier.weight(1f),
                onClick = onNavigateMicrophone
            )

            // Speaker Tile
            PeripheralTile(
                title = "Speaker",
                iconResId = R.drawable.ic_speaker,
                isActive = spkOn,
                modifier = Modifier.weight(1f),
                onClick = onNavigateSpeaker
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3RD ROW: CAMERA & REQUEST TOOL (+) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera (Webcam) Tile
            PeripheralTile(
                title = "Camera",
                iconResId = R.drawable.ic_camera,
                isActive = camOn,
                modifier = Modifier.weight(1f),
                onClick = onNavigateCamera
            )

            // Request Tool (+) Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .clickable { showRequestToolDialog = true },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyLight),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WhiteSurface)
                            .border(1.dp, CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Peripheral",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "More Tools",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("PC Connection Active", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Text(
                    "You are currently connected to your PC. Would you like to disconnect?",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        service?.disconnect()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Disconnect", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Stay Connected", color = TextDark)
                }
            },
            containerColor = WhiteSurface
        )
    }

    // Request Tool Dialog
    if (showRequestToolDialog) {
        AlertDialog(
            onDismissRequest = { showRequestToolDialog = false },
            title = { Text("Upcoming Peripherals", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The following PC peripherals are planned next:", fontSize = 13.sp, color = TextMuted)
                    Text("🎮 Virtual Gamepad / Controller", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    Text("📋 Cross-Device Clipboard Sync", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    Text("📁 Fast WiFi File Beam & Drop", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    Text("👆 Presentation Laser Pointer", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRequestToolDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlack)
                ) {
                    Text("Got It", color = Color.White)
                }
            },
            containerColor = WhiteSurface
        )
    }
}

@Composable
private fun PeripheralTile(
    title: String,
    iconResId: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) CardActive else CardGrey
        ),
        border = if (isActive) BorderStroke(2.dp, BorderActive) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Badge at top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.lowercase() + "...",
                    fontSize = 11.sp,
                    color = if (isActive) TextDark else TextMuted
                )

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (isActive) BorderActive else CardGreyLight,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = if (isActive) "ON" else "OFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Center Peripheral Icon
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                tint = if (isActive) BorderActive else TextDark,
                modifier = Modifier.size(52.dp)
            )

            // Title
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }
    }
}

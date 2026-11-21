package app.plugless.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.plugless.model.QrPayload
import app.plugless.ui.theme.*
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    onQrScanned: (QrPayload) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }

    // Laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laserTransition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val executor = Executors.newSingleThreadExecutor()
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val mlScanner = BarcodeScanning.getClient(options)

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (!hasScanned && mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            mlScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawText = barcode.rawValue ?: continue
                                        try {
                                            val payload = parseQrText(rawText)
                                            if (payload != null && !hasScanned) {
                                                hasScanned = true
                                                vibrate(ctx)
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onQrScanned(payload)
                                                }
                                                break
                                            }
                                        } catch (e: Exception) {
                                            Log.e("QrScanner", "Parse error: $rawText", e)
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Camera bind failure", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with Viewfinder
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                Text(
                    text = "Scan Terminal QR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                IconButton(
                    onClick = {
                        torchEnabled = !torchEnabled
                        cameraControl?.enableTorch(torchEnabled)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Torch",
                        tint = Color.Black
                    )
                }
            }

            // Viewfinder Target Frame
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x22FFFFFF))
                    .border(2.dp, Color.White, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Scanning Laser
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (laserY * 270).dp)
                        .background(StatusGreen)
                )
            }

            // Bottom Actions: Helper Text + Manual Connect
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Point camera at 'plugless pair' terminal QR code",
                    color = Color.White,
                    fontSize = 13.sp
                )

                Button(
                    onClick = { showManualDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter IP Manually",
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Manual Connection Dialog
    if (showManualDialog) {
        var ipInput by remember { mutableStateOf("10.192.37.116") }
        var portInput by remember { mutableStateOf("8765") }
        var tokenInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = {
                Text("Direct IP Connect", fontWeight = FontWeight.Bold, color = TextDark)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the IP address shown in your PC terminal:",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it.trim() },
                        label = { Text("PC LAN IP") },
                        placeholder = { Text("10.192.37.116") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it.trim() },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it.trim() },
                            label = { Text("Token (Opt)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val port = portInput.toIntOrNull() ?: 8765
                        if (ipInput.isNotEmpty()) {
                            showManualDialog = false
                            onQrScanned(
                                QrPayload(
                                    ip = ipInput,
                                    port = port,
                                    audioPort = port + 1,
                                    token = tokenInput.ifEmpty { "manual_pair" }
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlack)
                ) {
                    Text("Connect", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = WhiteSurface
        )
    }
}

private fun parseQrText(raw: String): QrPayload? {
    return try {
        // Try JSON format
        val jsonPayload = Gson().fromJson(raw, QrPayload::class.java)
        if (jsonPayload.ip.isNotEmpty()) {
            return jsonPayload
        }
        null
    } catch (_: Exception) {
        // Try URI format e.g. plugless://10.192.37.116:8765?token=xxx
        try {
            if (raw.startsWith("plugless://") || raw.startsWith("ws://")) {
                val clean = raw.removePrefix("plugless://").removePrefix("ws://")
                val hostPort = clean.substringBefore("?").substringBefore("/")
                val host = hostPort.substringBefore(":")
                val port = hostPort.substringAfter(":", "8765").toIntOrNull() ?: 8765
                val token = if (raw.contains("token=")) raw.substringAfter("token=").substringBefore("&") else "token"
                QrPayload(ip = host, port = port, audioPort = port + 1, token = token)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(120)
        }
    }
}

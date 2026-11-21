package app.plugless.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.plugless.model.ToolType
import app.plugless.service.PluglessService
import app.plugless.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    service: PluglessService?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraActive by (service?.cameraEnabled ?: remember { MutableStateFlow(false) }).collectAsState()

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

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
                text = "Camera (PC Webcam)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            // Prominent ON / OFF Switch
            Switch(
                checked = cameraActive,
                onCheckedChange = { isChecked ->
                    service?.setToolEnabled(ToolType.CAMERA, isChecked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CardBorder
                )
            )
        }

        // --- CAMERA PREVIEW / PLACEHOLDER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardGrey)
                .border(2.dp, if (cameraActive) BorderActive else CardBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (cameraActive) {
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
                            var lastFrameTs = 0L

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                val now = System.currentTimeMillis()
                                // Throttle to ~20 FPS for smooth LAN video
                                if (now - lastFrameTs >= 50) {
                                    lastFrameTs = now
                                    try {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && mediaImage.format == ImageFormat.YUV_420_888) {
                                            val yBuffer = mediaImage.planes[0].buffer
                                            val uBuffer = mediaImage.planes[1].buffer
                                            val vBuffer = mediaImage.planes[2].buffer

                                            val ySize = yBuffer.remaining()
                                            val uSize = uBuffer.remaining()
                                            val vSize = vBuffer.remaining()

                                            val nv21 = ByteArray(ySize + uSize + vSize)
                                            yBuffer.get(nv21, 0, ySize)
                                            vBuffer.get(nv21, ySize, vSize)
                                            uBuffer.get(nv21, ySize + vSize, uSize)

                                            val yuvImage = YuvImage(
                                                nv21,
                                                ImageFormat.NV21,
                                                imageProxy.width,
                                                imageProxy.height,
                                                null
                                            )
                                            val out = ByteArrayOutputStream()
                                            yuvImage.compressToJpeg(
                                                Rect(0, 0, imageProxy.width, imageProxy.height),
                                                60,
                                                out
                                            )
                                            val jpegBytes = out.toByteArray()
                                            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                                            service?.client?.sendCameraFrame(base64)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CameraScreen", "Frame error", e)
                                    }
                                }
                                imageProxy.close()
                            }

                            try {
                                cameraProvider.unbindAll()
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                            } catch (e: Exception) {
                                Log.e("CameraScreen", "Camera bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Lens switch & torch floating overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.Black
                        )
                    }

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
                            contentDescription = "Flashlight",
                            tint = Color.Black
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera is Inactive",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Turn the switch ON above to start webcam stream",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // --- INFO CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyLight)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PC Webcam Output",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "On your PC, open OBS Studio or browser at:",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhiteSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "http://localhost:8767/video",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = "Works with OBS Studio 'Start Virtual Camera' for Zoom, Discord & Google Meet.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

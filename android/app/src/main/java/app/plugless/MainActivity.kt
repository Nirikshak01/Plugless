package app.plugless

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import app.plugless.model.AppSettings
import app.plugless.model.ConnectionState
import app.plugless.model.QrPayload
import app.plugless.service.PluglessService
import app.plugless.ui.screens.*
import app.plugless.ui.theme.BlackBackground
import app.plugless.ui.theme.PluglessTheme
import com.google.gson.Gson

class MainActivity : ComponentActivity() {

    private var pluglessService: PluglessService? by mutableStateOf(null)
    private var currentScreen by mutableStateOf("home") // "home", "touchpad", "keyboard", "scanner", "settings"
    private var appSettings by mutableStateOf(AppSettings())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as PluglessService.LocalBinder
            pluglessService = localBinder.getService()
            pluglessService?.onStateChanged = {
                // Force recomposition
                pluglessService = pluglessService
            }

            // Auto-connect if configured and last host exists
            if (appSettings.autoConnect && appSettings.lastHostIp != null && appSettings.lastToken != null) {
                if (pluglessService?.connectionState == ConnectionState.DISCONNECTED) {
                    pluglessService?.connect(
                        hostIp = appSettings.lastHostIp!!,
                        wsPort = 8765,
                        audioPort = 8766,
                        token = appSettings.lastToken!!,
                        settings = appSettings
                    )
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            pluglessService = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && currentScreen == "request_camera") {
            currentScreen = "scanner"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSettings()

        // Bind Foreground Service
        val intent = Intent(this, PluglessService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Request initial permissions
        requestRequiredPermissions()

        setContent {
            if (appSettings.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            PluglessTheme(themeMode = appSettings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackBackground
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screenTransition"
                    ) { screen ->
                        when (screen) {
                            "home" -> HomeScreen(
                                service = pluglessService,
                                onNavigateTouchpad = { currentScreen = "touchpad" },
                                onNavigateKeyboard = { currentScreen = "keyboard" },
                                onNavigateScanner = {
                                    if (hasCameraPermission()) {
                                        currentScreen = "scanner"
                                    } else {
                                        currentScreen = "request_camera"
                                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                    }
                                },
                                onNavigateSettings = { currentScreen = "settings" }
                            )

                            "touchpad" -> TouchpadScreen(
                                service = pluglessService,
                                settings = appSettings,
                                onBack = { currentScreen = "home" }
                            )

                            "keyboard" -> KeyboardScreen(
                                service = pluglessService,
                                onBack = { currentScreen = "home" }
                            )

                            "scanner" -> QrScannerScreen(
                                onQrScanned = { payload ->
                                    handleQrScanned(payload)
                                    currentScreen = "home"
                                },
                                onBack = { currentScreen = "home" }
                            )

                            "settings" -> SettingsScreen(
                                settings = appSettings,
                                onSettingsChanged = { newSettings ->
                                    appSettings = newSettings
                                    saveSettings(newSettings)
                                },
                                onBack = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleQrScanned(payload: QrPayload) {
        val updated = appSettings.copy(
            lastHostIp = payload.ip,
            lastToken = payload.token
        )
        appSettings = updated
        saveSettings(updated)

        pluglessService?.connect(
            hostIp = payload.ip,
            wsPort = payload.port,
            audioPort = payload.audioPort,
            token = payload.token,
            settings = updated
        )
        Toast.makeText(this, "Connecting to PC at ${payload.ip}...", Toast.LENGTH_SHORT).show()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("plugless_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("settings", null)
        if (json != null) {
            try {
                appSettings = Gson().fromJson(json, AppSettings::class.java)
            } catch (_: Exception) {}
        }
    }

    private fun saveSettings(settings: AppSettings) {
        val prefs = getSharedPreferences("plugless_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("settings", Gson().toJson(settings)).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}

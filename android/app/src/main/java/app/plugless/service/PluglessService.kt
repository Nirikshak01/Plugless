package app.plugless.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.plugless.MainActivity
import app.plugless.R
import app.plugless.model.AppSettings
import app.plugless.model.ConnectionState
import app.plugless.model.ToolType
import app.plugless.network.AudioStreamManager
import app.plugless.network.PluglessClient
import kotlinx.coroutines.*

class PluglessService : Service() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var client: PluglessClient? = null
        private set
    var audioManager: AudioStreamManager? = null
        private set

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set
    var latencyMs: Long = 0L
        private set
    var mouseEnabled: Boolean = false
        private set
    var keyboardEnabled: Boolean = false
        private set
    var micEnabled: Boolean = false
        private set
    var speakerEnabled: Boolean = false
        private set
    var cameraEnabled: Boolean = false
        private set

    var onStateChanged: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): PluglessService = this@PluglessService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun connect(hostIp: String, wsPort: Int, audioPort: Int, token: String, settings: AppSettings) {
        disconnect()

        audioManager = AudioStreamManager(hostIp, audioPort, settings)

        client = PluglessClient(
            hostIp = hostIp,
            wsPort = wsPort,
            audioPort = audioPort,
            token = token,
            onConnectionStateChanged = { state ->
                scope.launch {
                    connectionState = state
                    updateNotification()
                    onStateChanged?.invoke()

                    if (state == ConnectionState.DISCONNECTED) {
                        audioManager?.stopMicrophone()
                        audioManager?.stopSpeaker()
                    }
                }
            },
            onStateSync = { m, k, mic, spk, cam ->
                scope.launch {
                    mouseEnabled = m
                    keyboardEnabled = k
                    micEnabled = mic
                    speakerEnabled = spk
                    cameraEnabled = cam

                    // Handle audio stream toggles
                    if (mic) audioManager?.startMicrophone() else audioManager?.stopMicrophone()
                    if (spk) audioManager?.startSpeaker() else audioManager?.stopSpeaker()

                    updateNotification()
                    onStateChanged?.invoke()
                }
            },
            onLatencyUpdated = { lat ->
                scope.launch {
                    latencyMs = lat
                    onStateChanged?.invoke()
                }
            }
        )

        client?.connect()
        startForeground(NOTIFICATION_ID, buildNotification("Connecting to PC..."))
    }

    fun setToolEnabled(tool: ToolType, enabled: Boolean) {
        when (tool) {
            ToolType.MOUSE -> {
                mouseEnabled = enabled
                client?.setToolState(ToolType.MOUSE, enabled)
            }
            ToolType.KEYBOARD -> {
                keyboardEnabled = enabled
                client?.setToolState(ToolType.KEYBOARD, enabled)
            }
            ToolType.MIC -> {
                micEnabled = enabled
                client?.setToolState(ToolType.MIC, enabled)
                if (enabled) audioManager?.startMicrophone() else audioManager?.stopMicrophone()
            }
            ToolType.SPEAKER -> {
                speakerEnabled = enabled
                client?.setToolState(ToolType.SPEAKER, enabled)
                if (enabled) audioManager?.startSpeaker() else audioManager?.stopSpeaker()
            }
            ToolType.CAMERA -> {
                cameraEnabled = enabled
                client?.setToolState(ToolType.CAMERA, enabled)
            }
        }
        updateNotification()
        onStateChanged?.invoke()
    }

    fun toggleTool(tool: ToolType) {
        when (tool) {
            ToolType.MOUSE -> setToolEnabled(ToolType.MOUSE, !mouseEnabled)
            ToolType.KEYBOARD -> setToolEnabled(ToolType.KEYBOARD, !keyboardEnabled)
            ToolType.MIC -> setToolEnabled(ToolType.MIC, !micEnabled)
            ToolType.SPEAKER -> setToolEnabled(ToolType.SPEAKER, !speakerEnabled)
            ToolType.CAMERA -> setToolEnabled(ToolType.CAMERA, !cameraEnabled)
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        audioManager?.release()
        audioManager = null
        connectionState = ConnectionState.DISCONNECTED
        mouseEnabled = false
        keyboardEnabled = false
        micEnabled = false
        speakerEnabled = false
        cameraEnabled = false
        onStateChanged?.invoke()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Plugless Peripheral Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background peripheral connection and streaming"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = when (connectionState) {
            ConnectionState.CONNECTED -> {
                val activeList = mutableListOf<String>()
                if (mouseEnabled) activeList.add("Mouse")
                if (keyboardEnabled) activeList.add("Keyboard")
                if (micEnabled) activeList.add("Mic")
                if (speakerEnabled) activeList.add("Speaker")
                if (cameraEnabled) activeList.add("Camera")

                if (activeList.isEmpty()) {
                    "Connected to PC (Idle)"
                } else {
                    "Active: ${activeList.joinToString(", ")}"
                }
            }
            ConnectionState.CONNECTING -> "Connecting to PC..."
            ConnectionState.DISCONNECTED -> "Disconnected"
        }
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Plugless")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        scope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "plugless_service_channel"
        const val NOTIFICATION_ID = 1001
    }
}

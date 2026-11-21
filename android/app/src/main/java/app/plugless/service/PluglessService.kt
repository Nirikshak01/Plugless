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
    var mouseEnabled: Boolean = true
        private set
    var keyboardEnabled: Boolean = true
        private set
    var micEnabled: Boolean = false
        private set
    var speakerEnabled: Boolean = false
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
            onStateSync = { m, k, mic, spk ->
                scope.launch {
                    mouseEnabled = m
                    keyboardEnabled = k
                    micEnabled = mic
                    speakerEnabled = spk

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

    fun toggleTool(tool: ToolType) {
        when (tool) {
            ToolType.MOUSE -> client?.setToolState(ToolType.MOUSE, !mouseEnabled)
            ToolType.KEYBOARD -> client?.setToolState(ToolType.KEYBOARD, !keyboardEnabled)
            ToolType.MIC -> client?.setToolState(ToolType.MIC, !micEnabled)
            ToolType.SPEAKER -> client?.setToolState(ToolType.SPEAKER, !speakerEnabled)
        }
    }

    fun applyPreset(preset: String) {
        when (preset.lowercase()) {
            "meeting" -> {
                client?.setToolState(ToolType.MIC, true)
                client?.setToolState(ToolType.SPEAKER, true)
            }
            "remote" -> {
                client?.setToolState(ToolType.MOUSE, true)
                client?.setToolState(ToolType.KEYBOARD, true)
            }
            "all" -> {
                client?.setToolState(ToolType.MOUSE, true)
                client?.setToolState(ToolType.KEYBOARD, true)
                client?.setToolState(ToolType.MIC, true)
                client?.setToolState(ToolType.SPEAKER, true)
            }
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        audioManager?.release()
        audioManager = null
        connectionState = ConnectionState.DISCONNECTED
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
                description = "Keeps connection and audio streams alive in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Plugless Peripherals Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_plugless_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val text = when (connectionState) {
            ConnectionState.CONNECTED -> {
                val activeList = mutableListOf<String>()
                if (mouseEnabled) activeList.add("Mouse")
                if (keyboardEnabled) activeList.add("Keyboard")
                if (micEnabled) activeList.add("Mic")
                if (speakerEnabled) activeList.add("Speaker")
                "Connected • ${activeList.joinToString(", ")} ($latencyMs ms)"
            }
            ConnectionState.CONNECTING -> "Connecting to PC..."
            ConnectionState.DISCONNECTED -> "Disconnected"
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        scope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "plugless_foreground_channel"
        const val NOTIFICATION_ID = 1001
    }
}

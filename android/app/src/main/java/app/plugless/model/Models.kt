package app.plugless.model

import com.google.gson.annotations.SerializedName

enum class ToolType {
    @SerializedName("mouse") MOUSE,
    @SerializedName("keyboard") KEYBOARD,
    @SerializedName("mic") MIC,
    @SerializedName("speaker") SPEAKER
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class QrPayload(
    val proto: String? = null,
    val version: String? = null,
    val ip: String,
    val port: Int = 8765,
    @SerializedName("audio_port") val audioPort: Int = 8766,
    val token: String
)

data class PeripheralState(
    val mouseEnabled: Boolean = true,
    val keyboardEnabled: Boolean = true,
    val micEnabled: Boolean = false,
    val speakerEnabled: Boolean = false,
    val latencyMs: Long = 0L,
    val connectedHost: String? = null
)

data class AppSettings(
    val themeMode: String = "dark", // "system", "light", "dark"
    val mouseSensitivity: Float = 1.0f,
    val scrollSpeed: Float = 1.0f,
    val naturalScroll: Boolean = true,
    val noiseSuppression: Boolean = true,
    val echoCancellation: Boolean = true,
    val micGain: Float = 1.0f,
    val speakerVolume: Float = 1.0f,
    val audioQuality: String = "balanced", // "low", "balanced", "high"
    val autoConnect: Boolean = true,
    val keepScreenOn: Boolean = true,
    val hapticFeedback: Boolean = true,
    val lastHostIp: String? = null,
    val lastToken: String? = null
)

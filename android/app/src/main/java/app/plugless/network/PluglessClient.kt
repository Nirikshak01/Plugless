package app.plugless.network

import android.os.Build
import android.util.Log
import app.plugless.model.ConnectionState
import app.plugless.model.ToolType
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class PluglessClient(
    private val hostIp: String,
    private val wsPort: Int = 8765,
    private val audioPort: Int = 8766,
    private val token: String,
    private val onConnectionStateChanged: (ConnectionState) -> Unit,
    private val onStateSync: (mouse: Boolean, kb: Boolean, mic: Boolean, spk: Boolean, cam: Boolean) -> Unit,
    private val onLatencyUpdated: (Long) -> Unit
) {
    private val tag = "PluglessClient"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pingJob: Job? = null
    private var isClosed = false

    fun connect() {
        isClosed = false
        onConnectionStateChanged(ConnectionState.CONNECTING)
        val url = "ws://$hostIp:$wsPort"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Connected to $url. Authenticating...")
                // Send Auth frame
                val authObj = JsonObject().apply {
                    addProperty("type", "auth")
                    addProperty("token", token)
                    addProperty("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                }
                webSocket.send(gson.toJson(authObj))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = gson.fromJson(text, JsonObject::class.java)
                    val type = root.get("type")?.asString ?: return

                    when (type) {
                        "auth_response" -> {
                            val success = root.get("success")?.asBoolean ?: false
                            if (success) {
                                onConnectionStateChanged(ConnectionState.CONNECTED)
                                startPingLoop()
                            } else {
                                Log.e(tag, "Auth failed: ${root.get("message")?.asString}")
                                onConnectionStateChanged(ConnectionState.DISCONNECTED)
                            }
                        }
                        "state_sync" -> {
                            val m = root.get("mouse")?.asBoolean ?: true
                            val k = root.get("keyboard")?.asBoolean ?: true
                            val mic = root.get("mic")?.asBoolean ?: false
                            val spk = root.get("speaker")?.asBoolean ?: false
                            val cam = root.get("camera")?.asBoolean ?: false
                            onStateSync(m, k, mic, spk, cam)
                        }
                        "pong" -> {
                            val clientTs = root.get("client_ts")?.asLong ?: 0L
                            val now = System.currentTimeMillis()
                            val rtt = (now - clientTs).coerceAtLeast(1L)
                            onLatencyUpdated(rtt)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing message: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket failure: ${t.message}")
                onConnectionStateChanged(ConnectionState.DISCONNECTED)
                stopPingLoop()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed: $reason ($code)")
                onConnectionStateChanged(ConnectionState.DISCONNECTED)
                stopPingLoop()
            }
        })
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && !isClosed) {
                val pingObj = JsonObject().apply {
                    addProperty("type", "ping")
                    addProperty("client_ts", System.currentTimeMillis())
                }
                webSocket?.send(gson.toJson(pingObj))
                delay(2000)
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    // Input Transmission API
    fun sendMouseMove(dx: Float, dy: Float) {
        val obj = JsonObject().apply {
            addProperty("type", "mouse_move")
            addProperty("dx", dx)
            addProperty("dy", dy)
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun sendMouseClick(button: String, state: String = "click") {
        val obj = JsonObject().apply {
            addProperty("type", "mouse_click")
            addProperty("button", button.lowercase())
            addProperty("state", state.lowercase())
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun sendMouseScroll(dx: Float, dy: Float) {
        val obj = JsonObject().apply {
            addProperty("type", "mouse_scroll")
            addProperty("dx", dx)
            addProperty("dy", dy)
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun sendKey(keyCode: Int, state: String = "click") {
        val obj = JsonObject().apply {
            addProperty("type", "key_press")
            addProperty("key_code", keyCode)
            addProperty("state", state.lowercase())
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun sendText(text: String) {
        val obj = JsonObject().apply {
            addProperty("type", "key_text")
            addProperty("text", text)
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun sendSpecialKey(key: String, state: String = "click") {
        val obj = JsonObject().apply {
            addProperty("type", "special_key")
            addProperty("key", key)
            addProperty("state", state.lowercase())
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun sendCameraFrame(base64Data: String) {
        val obj = JsonObject().apply {
            addProperty("type", "camera_frame")
            addProperty("data", base64Data)
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun setToolState(tool: ToolType, enabled: Boolean) {
        val obj = JsonObject().apply {
            addProperty("type", "set_tool_state")
            addProperty("tool", tool.name.lowercase())
            addProperty("enabled", enabled)
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun disconnect() {
        isClosed = true
        stopPingLoop()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        onConnectionStateChanged(ConnectionState.DISCONNECTED)
    }
}

package app.plugless.network

import android.annotation.SuppressLint
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import app.plugless.model.AppSettings
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioStreamManager(
    private val hostIp: String,
    private val audioPort: Int = 8766,
    private val settings: AppSettings
) {
    private val tag = "AudioStreamManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var micJob: Job? = null
    private var speakerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var udpSocket: DatagramSocket? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    companion object {
        const val SAMPLE_RATE = 48000
        const val MIC_CHUNK_SAMPLES = 960 // 20ms
        const val MIC_CHUNK_BYTES = MIC_CHUNK_SAMPLES * 2 // 1920 bytes mono 16-bit
        const val SPEAKER_CHUNK_BYTES = MIC_CHUNK_SAMPLES * 2 * 2 // 3840 bytes stereo 16-bit
    }

    private fun getSocket(): DatagramSocket {
        if (udpSocket == null || udpSocket!!.isClosed) {
            udpSocket = DatagramSocket()
        }
        return udpSocket!!
    }

    private fun startUdpHeartbeat() {
        if (heartbeatJob != null && heartbeatJob!!.isActive) return
        heartbeatJob = scope.launch {
            val targetAddr = InetAddress.getByName(hostIp)
            val pingBuffer = ByteArray(8)
            val header = ByteBuffer.wrap(pingBuffer).order(ByteOrder.BIG_ENDIAN)
            header.put(0, 0x03.toByte()) // 0x03 = AUDIO_PACKET_PING
            header.put(1, 0.toByte())

            while (isActive) {
                try {
                    val sock = getSocket()
                    if (!sock.isClosed) {
                        val nowMs = (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()
                        header.putInt(4, nowMs)
                        val pingPacket = DatagramPacket(pingBuffer, pingBuffer.size, targetAddr, audioPort)
                        sock.send(pingPacket)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "UDP Heartbeat error: ${e.message}")
                }
                delay(1200)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startMicrophone() {
        if (micJob != null && micJob!!.isActive) return

        startUdpHeartbeat()

        micJob = scope.launch {
            Log.d(tag, "Starting Phone Microphone stream...")
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBuf.coerceAtLeast(MIC_CHUNK_BYTES * 4)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                }

                val audioSessionId = audioRecord!!.audioSessionId

                // Enable hardware acoustic echo cancellation if supported & enabled
                if (settings.echoCancellation && AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                        enabled = true
                    }
                    Log.d(tag, "Hardware AEC enabled")
                }

                // Enable hardware noise suppression if supported & enabled
                if (settings.noiseSuppression && NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                        enabled = true
                    }
                    Log.d(tag, "Hardware Noise Suppressor enabled")
                }

                audioRecord!!.startRecording()
                val socket = getSocket()
                val targetAddr = InetAddress.getByName(hostIp)

                val pcmBuffer = ByteArray(MIC_CHUNK_BYTES)
                val packetBuffer = ByteArray(8 + MIC_CHUNK_BYTES)
                var seq: Short = 0

                while (isActive) {
                    val read = audioRecord!!.read(pcmBuffer, 0, MIC_CHUNK_BYTES)
                    if (read > 0) {
                        // Apply software gain if configured
                        if (settings.micGain != 1.0f) {
                            applyGain(pcmBuffer, read, settings.micGain)
                        }

                        // Build UDP packet: [type (1B), flags (1B), seq (2B), ts (4B)] + payload
                        val nowMs = (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()
                        val header = ByteBuffer.wrap(packetBuffer).order(ByteOrder.BIG_ENDIAN)
                        header.put(0, 0x01.toByte()) // 0x01 = MIC
                        header.put(1, 0.toByte())
                        header.putShort(2, seq)
                        header.putInt(4, nowMs)
                        System.arraycopy(pcmBuffer, 0, packetBuffer, 8, read)

                        val packet = DatagramPacket(packetBuffer, 8 + read, targetAddr, audioPort)
                        socket.send(packet)
                        seq = (seq + 1).toShort()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Microphone streaming error", e)
            } finally {
                stopMicrophoneInternal()
            }
        }
    }

    fun stopMicrophone() {
        micJob?.cancel()
        micJob = null
        stopMicrophoneInternal()
        if (speakerJob == null || !speakerJob!!.isActive) {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    private fun stopMicrophoneInternal() {
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            echoCanceler?.release()
            echoCanceler = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(tag, "Microphone stopped.")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing audioRecord", e)
        }
    }

    fun startSpeaker() {
        if (speakerJob != null && speakerJob!!.isActive) return

        startUdpHeartbeat()

        speakerJob = scope.launch {
            Log.d(tag, "Starting Speaker playback...")
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBuf.coerceAtLeast(SPEAKER_CHUNK_BYTES * 4)

            try {
                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioTrack?.setVolume(settings.speakerVolume.coerceIn(0f, 1f))
                }

                audioTrack!!.play()
                val socket = getSocket()
                val targetAddr = InetAddress.getByName(hostIp)

                // Send immediate registration ping so PC learns our UDP endpoint at once
                try {
                    val pingBuffer = ByteArray(8).apply { this[0] = 0x03.toByte() }
                    socket.send(DatagramPacket(pingBuffer, pingBuffer.size, targetAddr, audioPort))
                } catch (e: Exception) {
                    Log.w(tag, "Immediate speaker registration ping failed: ${e.message}")
                }

                val rxBuffer = ByteArray(4096)
                val packet = DatagramPacket(rxBuffer, rxBuffer.size)

                while (isActive) {
                    // Critical: Reset buffer length before every receive!
                    packet.length = rxBuffer.size
                    socket.receive(packet)
                    val len = packet.length
                    if (len > 8 && rxBuffer[0] == 0x02.toByte()) {
                        // 0x02 = Speaker audio from PC
                        val payloadLen = len - 8
                        audioTrack?.write(rxBuffer, 8, payloadLen)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Speaker streaming error", e)
            } finally {
                stopSpeakerInternal()
            }
        }
    }

    fun stopSpeaker() {
        speakerJob?.cancel()
        speakerJob = null
        stopSpeakerInternal()
        if (micJob == null || !micJob!!.isActive) {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    private fun stopSpeakerInternal() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            Log.d(tag, "Speaker stopped.")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing audioTrack", e)
        }
    }

    private fun applyGain(buffer: ByteArray, len: Int, gain: Float) {
        val bb = ByteBuffer.wrap(buffer, 0, len).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until len / 2) {
            val sample = bb.getShort(i * 2).toInt()
            val amplified = (sample * gain).toInt().coerceIn(-32768, 32767).toShort()
            bb.putShort(i * 2, amplified)
        }
    }

    fun release() {
        stopMicrophone()
        stopSpeaker()
        heartbeatJob?.cancel()
        heartbeatJob = null
        udpSocket?.close()
        udpSocket = null
    }
}

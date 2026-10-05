package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Real-Time Cloud Call Media Streamer (Voice & Video Frames over WebSocket Relay).
 *
 * Streams live microphone PCM audio chunks and compressed JPEG camera frames
 * between two real ZallCall devices connected in an active call session!
 */
class LiveCallStreamEngine {

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val postClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private var streamWebSocket: WebSocket? = null
    private var audioTransmitJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var lastFrameSentMs: Long = 0L

    private val _remoteVideoFrame = MutableStateFlow<Bitmap?>(null)
    val remoteVideoFrame: StateFlow<Bitmap?> = _remoteVideoFrame.asStateFlow()

    private val _packetsTransferred = MutableStateFlow(0)
    val packetsTransferred: StateFlow<Int> = _packetsTransferred.asStateFlow()

    private fun callRoomTopic(zallIdA: String, zallIdB: String): String {
        val sorted = listOf(
            zallIdA.trim().uppercase().replace(Regex("[^A-Z0-9]"), ""),
            zallIdB.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        ).sorted()
        return "zallcall_live_call_${sorted[0]}_${sorted[1]}"
    }

    fun startLiveCallStream(
        scope: CoroutineScope,
        myZallId: String,
        peerZallId: String,
        isMutedProvider: () -> Boolean
    ) {
        stopLiveCallStream()
        val roomTopic = callRoomTopic(myZallId, peerZallId)

        // 1. Initialize real AudioTrack speaker output for incoming voice packets
        val sampleRate = 8000
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(1600)

            @Suppress("DEPRECATION")
            val track = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf,
                AudioTrack.MODE_STREAM
            )
            track.play()
            audioTrack = track
        } catch (_: Exception) {
            audioTrack = null
        }

        // 2. Connect WebSocket to the shared Call Room Topic to receive real-time audio & video frames from peer
        val wsReq = Request.Builder()
            .url("wss://ntfy.sh/$roomTopic/ws")
            .build()

        streamWebSocket = wsClient.newWebSocket(wsReq, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    // Quick extract of "message":"..." from ntfy JSON event
                    val marker = "\"message\":\""
                    val startIdx = text.indexOf(marker)
                    if (startIdx == -1) return
                    val contentStart = startIdx + marker.length
                    val endIdx = text.indexOf("\"", contentStart)
                    if (endIdx == -1) return
                    val rawPayload = text.substring(contentStart, endIdx)

                    // Format: SENDER_ID|TYPE|BASE64_DATA
                    val parts = rawPayload.split("|", limit = 3)
                    if (parts.size < 3) return
                    val sender = parts[0]
                    if (sender.equals(myZallId, ignoreCase = true)) return

                    val type = parts[1]
                    val b64 = parts[2]
                    _packetsTransferred.value += 1

                    when (type) {
                        "AUD" -> {
                            val pcmBytes = Base64.decode(b64, Base64.DEFAULT)
                            audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                        }
                        "VID" -> {
                            val jpgBytes = Base64.decode(b64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.size)
                            if (bmp != null) {
                                _remoteVideoFrame.value = bmp
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
        })

        // 3. Start Microphone PCM capture loop and push audio chunks to peer
        audioTransmitJob = scope.launch(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(3200)

                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    val buffer = ByteArray(3200) // ~200ms of 8kHz 16-bit mono audio
                    while (isActive) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0 && !isMutedProvider()) {
                            val b64 = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
                            val packet = "$myZallId|AUD|$b64"
                            publishRawStreamPacket(roomTopic, packet)
                            _packetsTransferred.value += 1
                        }
                        delay(450L)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Sends a compressed JPEG video frame from the local CameraX PreviewView to the remote peer.
     */
    fun pushLocalVideoFrameIfReady(
        scope: CoroutineScope,
        myZallId: String,
        peerZallId: String,
        frameBitmap: Bitmap?
    ) {
        if (frameBitmap == null) return
        val now = System.currentTimeMillis()
        if (now - lastFrameSentMs < 950L) return // ~1 fps lightweight cloud video relay
        lastFrameSentMs = now

        val roomTopic = callRoomTopic(myZallId, peerZallId)
        scope.launch(Dispatchers.IO) {
            try {
                val scaled = Bitmap.createScaledBitmap(frameBitmap, 160, 220, true)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 52, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                val packet = "$myZallId|VID|$b64"
                publishRawStreamPacket(roomTopic, packet)
                _packetsTransferred.value += 1
            } catch (_: Exception) {
            }
        }
    }

    private fun publishRawStreamPacket(roomTopic: String, payload: String) {
        try {
            val req = Request.Builder()
                .url("https://ntfy.sh/$roomTopic")
                .post(payload.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()
            postClient.newCall(req).execute().close()
        } catch (_: Exception) {
        }
    }

    fun stopLiveCallStream() {
        audioTransmitJob?.cancel()
        audioTransmitJob = null
        try {
            streamWebSocket?.close(1000, "call_ended")
        } catch (_: Exception) {
        }
        streamWebSocket = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        _remoteVideoFrame.value = null
        _packetsTransferred.value = 0
    }
}

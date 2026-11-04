package br.com.laboratorio.android.live

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Base64
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cliente Android da Gemini Live API.
 *
 * A chave permanente é usada somente para emitir um token efêmero via HTTPS.
 * O WebSocket recebe o token de curta duração, nunca a API key permanente.
 * Microfone: PCM 16 kHz / mono / 16-bit. Saída: PCM 24 kHz / mono / 16-bit.
 */
class GeminiLiveClient(
    context: Context,
    private val listener: Listener,
) {
    interface Listener {
        fun onStatus(message: String)
        fun onReady()
        fun onUserText(text: String)
        fun onModelText(text: String)
        fun onTurnComplete()
        fun onInterrupted()
        fun onInteractionStatus(status: String)
        fun onNotice(message: String)
        fun onSessionEnded(message: String)
        fun onError(message: String)
    }

    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // A emissão do token é HTTP finito e precisa de limites próprios.
    private val authHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    // A conexão Live é persistente; readTimeout=0 é intencional somente para WebSocket.
    private val liveSocketClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Volatile private var running = false
    @Volatile private var setupComplete = false
    @Volatile private var muted = false
    @Volatile private var socket: WebSocket? = null
    @Volatile private var tokenCall: Call? = null

    private var tokenJob: Job? = null
    private var setupTimeoutJob: Job? = null
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var microphoneThread: Thread? = null
    private var playbackThread: Thread? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var currentSettings: LiveSettings? = null
    private val playbackQueue = BoundedAudioBuffer()

    fun start(settings: LiveSettings) {
        if (running) return
        if (settings.apiKey.isBlank()) {
            listener.onError("Configure sua chave API antes de iniciar.")
            return
        }

        running = true
        setupComplete = false
        muted = false
        currentSettings = settings
        listener.onStatus("Solicitando token temporário ao Google…")

        tokenJob = scope.launch {
            try {
                val token = requestEphemeralToken(settings.apiKey)
                if (!running) return@launch
                listener.onStatus("Conectando ao Gemini Live…")
                openWebSocket(token, settings)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error.message ?: "Não foi possível autenticar no Gemini.")
            }
        }
    }

    fun setMuted(value: Boolean) {
        muted = value
    }

    @Synchronized
    fun stop() {
        val hadSession = running || socket != null
        running = false
        tokenJob?.cancel()
        tokenCall?.cancel()
        tokenCall = null
        setupTimeoutJob?.cancel()
        tokenJob = null
        setupTimeoutJob = null

        socket?.close(1000, "Sessão encerrada pelo usuário")
        socket = null
        releaseAudio()
        restoreAudioRoute()

        if (hadSession) listener.onSessionEnded("Desconectado")
    }

    fun close() {
        stop()
        scope.cancel()
        authHttpClient.dispatcher.executorService.shutdown()
        liveSocketClient.dispatcher.executorService.shutdown()
    }

    private fun requestEphemeralToken(apiKey: String): String {
        val now = java.time.Instant.now()
        val body = JSONObject()
            .put("uses", 1)
            .put("newSessionExpireTime", now.plusSeconds(60).toString())
            .put("expireTime", now.plusSeconds(30 * 60).toString())

        // O Google rejeitou liveConnectConstraints no fluxo real deste projeto.
        // O token continua limitado a um uso e 30 minutos; o modelo é definido no setup.
        val request = Request.Builder()
            .url(AUTH_TOKEN_URL)
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = authHttpClient.newCall(request)
        registerTokenCall(call)
        try {
            call.execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val providerError = runCatching {
                        JSONObject(raw).optJSONObject("error")
                    }.getOrNull()
                    val providerStatus = providerError?.optString("status").orEmpty()
                    val suffix = if (providerStatus.isNotBlank()) " — $providerStatus" else ""
                    throw IOException("Google recusou a chave API (HTTP ${response.code})$suffix.")
                }

                val token = JSONObject(raw).optString("name")
                if (token.isBlank()) throw IOException("O Google não retornou um token temporário.")
                return token
            }
        } finally {
            synchronized(this) {
                if (tokenCall === call) tokenCall = null
            }
        }
    }

    private fun registerTokenCall(call: Call) {
        synchronized(this) {
            if (!running) {
                call.cancel()
                throw CancellationException("A solicitação de token foi cancelada.")
            }
            tokenCall = call
        }
    }

    private fun openWebSocket(token: String, settings: LiveSettings) {
        // A documentação Gemini permite Authorization: Token para tokens efêmeros;
        // evita expor a credencial na URL e em logs de proxy/histórico de requisição.
        val request = Request.Builder()
            .url(LIVE_SOCKET_URL)
            .header("Authorization", "Token $token")
            .build()
        socket = liveSocketClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!running) {
                    webSocket.close(1000, "Sessão cancelada")
                    return
                }

                listener.onStatus("Enviando configuração da sessão…")
                setupTimeoutJob?.cancel()
                setupTimeoutJob = scope.launch {
                    delay(20_000)
                    if (running && !setupComplete) {
                        fail("Tempo esgotado aguardando a inicialização do Gemini.")
                    }
                }

                if (!webSocket.send(buildSetupMessage(settings).toString())) {
                    fail("Não foi possível enviar a configuração inicial ao Gemini.")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (running) handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (running) handleServerMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
                if (!running) return
                val code = response?.code
                val message = when (code) {
                    400, 401, 403 -> "A chave API foi recusada pelo Google (HTTP $code). Confira a chave e as permissões do projeto."
                    else -> "Falha na conexão com Gemini Live: ${sanitize(error.message ?: "erro de rede")}."
                }
                fail(message)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!running) return
                running = false
                setupTimeoutJob?.cancel()
                releaseAudio()
                restoreAudioRoute()
                listener.onSessionEnded(
                    if (reason.isBlank()) "Sessão encerrada (código $code)." else "Sessão encerrada: ${sanitize(reason)}"
                )
            }
        })
    }

    private fun buildSetupMessage(settings: LiveSettings): JSONObject {
        val generationConfig = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put(
                "speechConfig",
                JSONObject().put(
                    "voiceConfig",
                    JSONObject().put(
                        "prebuiltVoiceConfig",
                        JSONObject().put("voiceName", settings.voice),
                    ),
                ),
            )

        if (ModelCatalog.supportsThinking(settings.model)) {
            generationConfig.put(
                "thinkingConfig",
                JSONObject().put("thinkingLevel", settings.thinkingLevel.uppercase()),
            )
        }

        val setup = JSONObject()
            .put("model", "models/${settings.model}")
            .put("generationConfig", generationConfig)
            .put("inputAudioTranscription", JSONObject())
            .put("outputAudioTranscription", JSONObject())

        val systemInstruction = settings.effectiveSystemInstruction()
        if (systemInstruction.isNotBlank()) {
            setup.put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction)),
                ),
            )
        }

        return JSONObject().put("setup", setup)
    }

    private fun handleServerMessage(raw: String) {
        val packet = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return
        }

        val apiError = packet.optJSONObject("error")
            ?: packet.optJSONObject("serverContent")?.optJSONObject("error")
        if (apiError != null) {
            val code = apiError.optInt("code", 0)
            val status = apiError.optString("status", "ERRO")
            val detail = sanitize(apiError.optString("message", "Configuração recusada."))
            fail("Gemini recusou a sessão ($code $status): $detail")
            return
        }

        if (packet.has("setupComplete")) {
            setupTimeoutJob?.cancel()
            setupComplete = true
            try {
                startAudio(currentSettings?.speaker ?: true)
                listener.onReady()
            } catch (error: Exception) {
                fail("Não foi possível iniciar o áudio: ${sanitize(error.message ?: "dispositivo de áudio indisponível")}")
            }
            return
        }

        val content = packet.optJSONObject("serverContent")
        val interactionStatus = packet.optString("interactionStatus")
            .ifBlank { content?.optString("interactionStatus", "").orEmpty() }
        if (interactionStatus.isNotBlank()) {
            listener.onInteractionStatus(interactionStatus)
        }


        if (content == null) return

        if (content.optBoolean("interrupted", false)) {
            flushPlayback()
            listener.onInterrupted()
        }

        val userText = content.optJSONObject("inputTranscription")?.optString("text").orEmpty()
        if (userText.isNotBlank()) listener.onUserText(userText)

        val modelText = content.optJSONObject("outputTranscription")?.optString("text").orEmpty()
        if (modelText.isNotBlank()) listener.onModelText(modelText)

        val parts = content.optJSONObject("modelTurn")?.optJSONArray("parts")
        if (parts != null) {
            for (index in 0 until parts.length()) {
                val inlineData = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
                if (!inlineData.optString("mimeType").startsWith("audio/pcm")) continue
                val encoded = inlineData.optString("data")
                if (encoded.isNotBlank()) {
                    runCatching { Base64.decode(encoded, Base64.DEFAULT) }
                        .onSuccess { playbackQueue.offer(it) }
                }
            }
        }

        if (content.optBoolean("turnComplete", false)) {
            listener.onTurnComplete()
        }

        if (packet.has("goAway")) {
            listener.onNotice("O Google vai encerrar esta sessão em breve. Reinicie a conversa se necessário.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudio(useSpeaker: Boolean) {
        if (!running) return
        playbackQueue.clear()
        routeAudio(useSpeaker)

        val outputMin = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (outputMin <= 0) throw IllegalStateException("Saída de áudio não suportada.")
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(outputMin, OUTPUT_SAMPLE_RATE / 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            throw IllegalStateException("Não foi possível inicializar o alto-falante.")
        }
        track.play()
        player = track

        playbackThread = Thread {
            while (running) {
                val chunk = try {
                    playbackQueue.poll(100, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    null
                } ?: continue
                val activeTrack = player ?: break
                runCatching { activeTrack.write(chunk, 0, chunk.size) }
            }
        }.also {
            it.name = "gemini-live-playback"
            it.start()
        }

        val inputMin = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (inputMin <= 0) throw IllegalStateException("Microfone não suporta PCM 16 kHz.")
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(inputMin * 2, MICROPHONE_CHUNK_BYTES * 4),
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            throw IllegalStateException("Não foi possível inicializar o microfone.")
        }

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)?.also {
                it.enabled = true
            }
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)?.also {
                it.enabled = true
            }
        }

        recorder = audioRecord
        audioRecord.startRecording()
        microphoneThread = Thread {
            val buffer = ByteArray(MICROPHONE_CHUNK_BYTES)
            while (running) {
                val count = audioRecord.read(buffer, 0, buffer.size)
                if (count < 0) break
                if (count == 0 || muted || !running) continue

                val encoded = Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP)
                val payload = JSONObject().put(
                    "realtimeInput",
                    JSONObject().put(
                        "audio",
                        JSONObject()
                            .put("mimeType", "audio/pcm;rate=16000")
                            .put("data", encoded),
                    ),
                )
                if (socket?.send(payload.toString()) == false) break
            }
        }.also {
            it.name = "gemini-live-microphone"
            it.start()
        }
    }

    private fun routeAudio(useSpeaker: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wantedType = if (useSpeaker) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            val device = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == wantedType
            }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            setLegacySpeakerphone(useSpeaker)
        }
    }

    private fun restoreAudioRoute() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                setLegacySpeakerphone(false)
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }

    @Suppress("DEPRECATION")
    private fun setLegacySpeakerphone(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
    }

    private fun flushPlayback() {
        playbackQueue.clear()
        val track = player ?: return
        runCatching {
            track.pause()
            track.flush()
            track.play()
        }
    }

    @Synchronized
    private fun fail(message: String) {
        if (!running) return
        running = false
        setupTimeoutJob?.cancel()
        tokenJob?.cancel()
        tokenCall?.cancel()
        tokenCall = null
        socket?.cancel()
        socket = null
        releaseAudio()
        restoreAudioRoute()
        listener.onError(sanitize(message))
    }

    private fun releaseAudio() {
        try { recorder?.stop() } catch (_: Exception) { }
        try { microphoneThread?.join(400) } catch (_: Exception) { }
        try { recorder?.release() } catch (_: Exception) { }
        try { echoCanceler?.release() } catch (_: Exception) { }
        try { noiseSuppressor?.release() } catch (_: Exception) { }
        recorder = null
        echoCanceler = null
        noiseSuppressor = null
        microphoneThread = null

        playbackQueue.clear()
        try { playbackThread?.join(400) } catch (_: Exception) { }
        try {
            player?.pause()
            player?.flush()
            player?.stop()
            player?.release()
        } catch (_: Exception) { }
        player = null
        playbackThread = null
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("""(?i)access_token=[^&\s]+"""), "access_token=[oculto]")
        .replace(Regex("""(?i)auth_tokens/[A-Za-z0-9_-]+"""), "auth_tokens/[oculto]")
        .replace(Regex("""AIza[\w-]+"""), "[chave API ocultada]")
        .replace(Regex("""(?i)bearer\s+\S+"""), "Bearer [oculto]")
        .replace(Regex("""(?i)authorization\s*[:=]\s*token\s+\S+"""), "Authorization: Token [oculto]")
        .replace(Regex("""(?i)\btoken\s+[A-Za-z0-9._~-]{24,}"""), "Token [oculto]")
        .take(180)

    private companion object {
        const val AUTH_TOKEN_URL = "https://generativelanguage.googleapis.com/v1beta/auth_tokens"
        const val LIVE_SOCKET_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained"
        const val INPUT_SAMPLE_RATE = 16_000
        const val OUTPUT_SAMPLE_RATE = 24_000
        const val MICROPHONE_CHUNK_BYTES = 1_280 // 40 ms de PCM mono 16-bit
    }
}

package br.com.laboratorio.android.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

private const val LIVE_MODEL = "gemini-3.8-live-extended-thinking"
private const val GEMINI_SOCKET = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContentConstrained"
private const val DEVICE_REFRESH_WINDOW_SECONDS = 30L * 24L * 60L * 60L

data class LiveUiState(
    val status: String = "Desconectado",
    val connected: Boolean = false,
    val starting: Boolean = false,
    val lastMessage: String = "",
    val reasoning: Boolean = false,
    val requiresPairing: Boolean = true
)

private class BrokerHttpException(
    val statusCode: Int,
    val endpoint: String
) : IOException("Servidor retornou HTTP $statusCode")

class LiveAudioClient(context: Context) {
    private val appContext = context.applicationContext
    private val credentialStore = DeviceCredentialStore(appContext)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val _state = MutableStateFlow(LiveUiState())
    val state: StateFlow<LiveUiState> = _state
    private var socket: WebSocket? = null
    private var recorder: AudioRecord? = null
    private var playback: AudioTrack? = null
    private var captureJob: Job? = null
    private var setupTimeoutJob: Job? = null
    private var preserveTerminalStatus = false
    private val recording = AtomicBoolean(false)
    private val incomingAudio = Channel<ByteArray>(24)

    init {
        io.launch {
            val hasCredential = runCatching { credentialStore.load() != null }.getOrDefault(false)
            _state.value = _state.value.copy(requiresPairing = !hasCredential)
            for (bytes in incomingAudio) {
                try {
                    playback?.write(bytes, 0, bytes.size)
                } catch (_: Exception) {
                    // O fluxo pode ser fechado durante a liberação do áudio.
                }
            }
        }
    }

    suspend fun connect(brokerUrl: String, pairingCode: String) {
        if (_state.value.starting || _state.value.connected) return
        preserveTerminalStatus = false
        setupTimeoutJob?.cancel()
        val base = brokerUrl.trimEnd('/')
        if (!base.startsWith("https://")) {
            _state.value = _state.value.copy(
                status = "O servidor de autenticação precisa usar HTTPS.",
                starting = false
            )
            return
        }
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = _state.value.copy(
                status = "Permissão de microfone necessária.",
                starting = false
            )
            return
        }

        _state.value = _state.value.copy(
            status = "Verificando autenticação do dispositivo…",
            starting = true,
            connected = false,
            lastMessage = ""
        )

        try {
            var credential = withContext(Dispatchers.IO) { credentialStore.load() }

            if (credential == null) {
                val code = pairingCode.trim()
                if (code.length < 32) {
                    _state.value = _state.value.copy(
                        status = "Ativação inicial necessária. Informe o código uma única vez.",
                        starting = false,
                        requiresPairing = true
                    )
                    return
                }
                val deviceId = withContext(Dispatchers.IO) {
                    credentialStore.getOrCreateDeviceId()
                }
                val enrollment = postBroker(
                    base,
                    "/auth/enroll",
                    code,
                    JSONObject().put("deviceId", deviceId)
                )
                credential = parseCredential(enrollment)
                withContext(Dispatchers.IO) { credentialStore.save(credential) }
                _state.value = _state.value.copy(
                    status = "Dispositivo ativado. Preparando Gemini Live…",
                    starting = true,
                    requiresPairing = false
                )
            } else {
                val secondsRemaining = credential.expiresAtEpochSeconds -
                    System.currentTimeMillis() / 1000L
                if (secondsRemaining <= DEVICE_REFRESH_WINDOW_SECONDS) {
                    val refreshed = postBroker(
                        base,
                        "/auth/refresh",
                        credential.token,
                        JSONObject()
                    )
                    credential = parseCredential(refreshed)
                    withContext(Dispatchers.IO) { credentialStore.save(credential) }
                    _state.value = _state.value.copy(
                        status = "Autorização renovada automaticamente.",
                        requiresPairing = false
                    )
                }
            }

            val activeCredential = credential
                ?: throw IOException("Credencial de dispositivo indisponível.")
            val tokenResponse = postBroker(
                base,
                "/live/token",
                activeCredential.token,
                JSONObject()
            )
            val token = tokenResponse.getString("token")
            if (token.isBlank()) throw IOException("O servidor não retornou um token temporário.")

            _state.value = _state.value.copy(
                status = "Conectando ao Gemini Live…",
                starting = true,
                requiresPairing = false
            )
            val request = Request.Builder()
                .url(GEMINI_SOCKET)
                .header("Authorization", "Token $token")
                .build()
            socket = http.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    setupTimeoutJob?.cancel()
                    setupTimeoutJob = io.launch {
                        delay(20_000)
                        if (_state.value.starting && !_state.value.connected) {
                            preserveTerminalStatus = true
                            _state.value = _state.value.copy(
                                status = "Tempo esgotado aguardando confirmação do Gemini. A sessão não foi inicializada.",
                                connected = false,
                                starting = false
                            )
                            webSocket.close(1000, "Timeout aguardando setupComplete")
                        }
                    }
                    val setup = JSONObject().put(
                        "setup",
                        JSONObject()
                            .put("model", "models/$LIVE_MODEL")
                            .put(
                                "generationConfig",
                                JSONObject()
                                    .put("responseModalities", org.json.JSONArray().put("AUDIO"))
                                    .put("thinkingConfig", JSONObject().put("thinkingLevel", "LOW"))
                                    .put(
                                        "speechConfig",
                                        JSONObject().put(
                                            "voiceConfig",
                                            JSONObject().put(
                                                "prebuiltVoiceConfig",
                                                JSONObject().put("voiceName", "Puck")
                                            )
                                        )
                                    )
                            )
                    )
                    webSocket.send(setup.toString())
                    _state.value = _state.value.copy(
                        status = "Aguardando inicialização da sessão…",
                        starting = true,
                        requiresPairing = false
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        processMessage(text)
                    } catch (_: Exception) {
                        _state.value = _state.value.copy(lastMessage = "Mensagem não reconhecida")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    setupTimeoutJob?.cancel()
                    haltAudio()
                    if (!preserveTerminalStatus) {
                        val httpStatus = response?.code?.let { " HTTP $it" }.orEmpty()
                        _state.value = _state.value.copy(
                            status = "Falha na sessão$httpStatus: ${t.message?.take(90) ?: "erro de conexão"}",
                            connected = false,
                            starting = false
                        )
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    setupTimeoutJob?.cancel()
                    haltAudio()
                    if (!preserveTerminalStatus) {
                        _state.value = _state.value.copy(
                            status = "Sessão encerrada ($code)",
                            connected = false,
                            starting = false
                        )
                    }
                }
            })
        } catch (error: BrokerHttpException) {
            if (error.statusCode == 401 &&
                (error.endpoint == "/auth/refresh" || error.endpoint == "/live/token")
            ) {
                withContext(Dispatchers.IO) { credentialStore.clear() }
                _state.value = LiveUiState(
                    status = "A autorização expirou ou foi revogada. Faça a ativação novamente.",
                    requiresPairing = true
                )
            } else if (error.statusCode == 401 && error.endpoint == "/auth/enroll") {
                _state.value = LiveUiState(
                    status = "Código de ativação inválido. Confira e tente novamente.",
                    requiresPairing = true
                )
            } else {
                _state.value = _state.value.copy(
                    status = error.message ?: "Falha no servidor de autenticação.",
                    starting = false,
                    connected = false
                )
            }
        } catch (error: Exception) {
            _state.value = _state.value.copy(
                status = "Não foi possível conectar: ${error.message?.take(100) ?: "erro inesperado"}",
                starting = false,
                connected = false
            )
        }
    }

    private suspend fun postBroker(
        base: String,
        endpoint: String,
        credential: String,
        body: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(base + endpoint)
            .header("Authorization", "Bearer $credential")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw BrokerHttpException(response.code, endpoint)
            JSONObject(responseBody)
        }
    }

    private fun parseCredential(response: JSONObject): DeviceCredential {
        val token = response.getString("deviceToken")
        val expiresAt = response.getLong("expiresAtEpochSeconds")
        if (token.isBlank() || expiresAt <= System.currentTimeMillis() / 1000L) {
            throw IOException("Credencial de dispositivo inválida.")
        }
        return DeviceCredential(token, expiresAt)
    }

    private fun processMessage(text: String) {
        val packet = JSONObject(text)
        val apiError = packet.optJSONObject("error")
            ?: packet.optJSONObject("serverContent")?.optJSONObject("error")
        if (apiError != null) {
            setupTimeoutJob?.cancel()
            preserveTerminalStatus = true
            val code = apiError.optInt("code", 0)
            val status = apiError.optString("status", "ERRO")
            val message = apiError.optString("message", "O Google recusou a configuração da sessão.")
                .replace(Regex("AIza[\\w-]+"), "[segredo oculto]")
                .replace(Regex("(?i)bearer\\s+\\S+"), "Bearer [credencial oculta]")
                .take(180)
            _state.value = _state.value.copy(
                status = "Gemini recusou a sessão ($code $status): $message",
                connected = false,
                starting = false,
                reasoning = false
            )
            socket?.close(1008, "Gemini recusou a configuração")
            return
        }
        if (packet.has("setupComplete")) {
            setupTimeoutJob?.cancel()
            preserveTerminalStatus = false
            _state.value = _state.value.copy(
                status = "Conectado — pode falar",
                connected = true,
                starting = false,
                requiresPairing = false
            )
            startAudio()
            return
        }
        val content = packet.optJSONObject("serverContent") ?: return
        val interaction = content.optString("interactionStatus", "")
        if (interaction == "IN_PROGRESS") {
            _state.value = _state.value.copy(
                status = "Gemini está raciocinando…",
                reasoning = true
            )
        } else if (interaction == "IDLE") {
            _state.value = _state.value.copy(
                status = "Conectado — pode falar",
                reasoning = false
            )
        }
        if (content.optBoolean("interrupted")) {
            while (incomingAudio.tryReceive().isSuccess) {
                // Descarta áudio da resposta interrompida.
            }
        }
        val transcription = content.optJSONObject("outputTranscription")?.optString("text")
        if (!transcription.isNullOrEmpty()) {
            _state.value = _state.value.copy(lastMessage = transcription.take(250))
        }
        val parts = content.optJSONObject("modelTurn")?.optJSONArray("parts") ?: return
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val audio = part.optJSONObject("inlineData") ?: continue
            if (audio.optString("mimeType").startsWith("audio/pcm")) {
                val chunk = Base64.decode(audio.getString("data"), Base64.DEFAULT)
                incomingAudio.trySend(chunk)
            }
        }
    }

    private fun startAudio() {
        if (!recording.compareAndSet(false, true)) return
        val playbackBuffer = max(
            AudioTrack.getMinBufferSize(
                24000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            9600
        )
        playback = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(24000)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(playbackBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        val minBuffer = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            _state.value = _state.value.copy(status = "Microfone indisponível")
            recording.set(false)
            return
        }
        try {
            @Suppress("MissingPermission")
            val input = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuffer * 2, 4096)
            )
            recorder = input
            input.startRecording()
            captureJob = io.launch {
                val buffer = ByteArray(2048)
                while (recording.get()) {
                    val count = input.read(buffer, 0, buffer.size)
                    if (count > 0 && recording.get()) {
                        val encoded = Base64.encodeToString(
                            buffer.copyOf(count),
                            Base64.NO_WRAP
                        )
                        val payload = JSONObject().put(
                            "realtimeInput",
                            JSONObject().put(
                                "audio",
                                JSONObject()
                                    .put("mimeType", "audio/pcm;rate=16000")
                                    .put("data", encoded)
                            )
                        )
                        socket?.send(payload.toString())
                    }
                }
            }
        } catch (_: Exception) {
            _state.value = _state.value.copy(status = "Falha ao iniciar microfone")
            haltAudio()
        }
    }

    private fun haltAudio() {
        recording.set(false)
        try { recorder?.stop() } catch (_: Exception) { }
        captureJob?.cancel()
        try { recorder?.release() } catch (_: Exception) { }
        recorder = null
        try { playback?.stop() } catch (_: Exception) { }
        try { playback?.release() } catch (_: Exception) { }
        playback = null
    }

    fun disconnect() {
        val requiresPairing = _state.value.requiresPairing
        setupTimeoutJob?.cancel()
        preserveTerminalStatus = false
        recording.set(false)
        socket?.close(1000, "Usuário encerrou a sessão")
        socket = null
        haltAudio()
        _state.value = LiveUiState(
            status = "Desconectado",
            requiresPairing = requiresPairing
        )
    }

    fun close() {
        disconnect()
        incomingAudio.close()
        io.cancel()
        http.dispatcher.executorService.shutdown()
    }
}

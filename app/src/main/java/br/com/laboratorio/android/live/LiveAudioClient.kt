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
private const val GEMINI_SOCKET = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

data class LiveUiState(
    val status: String = "Desconectado",
    val connected: Boolean = false,
    val starting: Boolean = false,
    val lastMessage: String = "",
    val reasoning: Boolean = false
)

class LiveAudioClient(private val context: Context) {
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
    private val recording = AtomicBoolean(false)
    private val incomingAudio = Channel<ByteArray>(24)

    init {
        io.launch {
            for (bytes in incomingAudio) {
                try { playback?.write(bytes, 0, bytes.size) } catch (_: Exception) { /* encerrando */ }
            }
        }
    }

    suspend fun connect(brokerUrl: String, labCode: String) {
        if (_state.value.starting || _state.value.connected) return
        if (!brokerUrl.startsWith("https://") || labCode.length < 32) {
            _state.value = LiveUiState(status = "Informe URL HTTPS e código de acesso válido")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = LiveUiState(status = "Permissão de microfone necessária")
            return
        }
        _state.value = LiveUiState(status = "Solicitando token temporário…", starting = true)
        try {
            val base = brokerUrl.trimEnd('/')
            val token = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("$base/live/token")
                    .header("Authorization", "Bearer $labCode")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Servidor retornou ${response.code}")
                    JSONObject(response.body?.string().orEmpty()).getString("token")
                }
            }
            _state.value = _state.value.copy(status = "Conectando ao Gemini Live…")
            val req = Request.Builder().url(GEMINI_SOCKET)
                .header("Authorization", "Token $token")
                .build()
            socket = http.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val setup = JSONObject().put("setup", JSONObject()
                        .put("model", "models/$LIVE_MODEL")
                        .put("generationConfig", JSONObject()
                            .put("responseModalities", org.json.JSONArray().put("AUDIO"))
                            .put("thinkingConfig", JSONObject().put("thinkingLevel", "LOW"))
                            .put("speechConfig", JSONObject().put("voiceConfig", JSONObject()
                                .put("prebuiltVoiceConfig", JSONObject().put("voiceName", "Puck"))))))
                    webSocket.send(setup.toString())
                    _state.value = LiveUiState(status = "Aguardando inicialização da sessão…", starting = true)
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try { processMessage(text) } catch (_: Exception) {
                        _state.value = _state.value.copy(lastMessage = "Mensagem não reconhecida")
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    haltAudio()
                    _state.value = LiveUiState(status = "Falha na sessão: ${t.message?.take(90) ?: "erro de conexão"}")
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    haltAudio()
                    _state.value = LiveUiState(status = "Sessão encerrada")
                }
            })
        } catch (error: Exception) {
            _state.value = LiveUiState(status = "Não foi possível conectar: ${error.message?.take(100)}")
        }
    }

    private fun processMessage(text: String) {
        val packet = JSONObject(text)
        if (packet.has("setupComplete")) {
            _state.value = LiveUiState(status = "Conectado — pode falar", connected = true)
            startAudio()
            return
        }
        // O estado da interação no Gemini 3.8 Live chega dentro de serverContent,
        // e não no objeto de evento superior. turnComplete não implica IDLE.
        val content = packet.optJSONObject("serverContent") ?: return
        val interaction = content.optString("interactionStatus", "")
        if (interaction == "IN_PROGRESS") {
            _state.value = _state.value.copy(status = "Gemini está raciocinando…", reasoning = true)
        } else if (interaction == "IDLE") {
            _state.value = _state.value.copy(status = "Conectado — pode falar", reasoning = false)
        }
        if (content.optBoolean("interrupted")) {
            while (incomingAudio.tryReceive().isSuccess) { /* descarta áudio interrompido */ }
        }
        val transcription = content.optJSONObject("outputTranscription")?.optString("text")
        if (!transcription.isNullOrEmpty()) _state.value = _state.value.copy(lastMessage = transcription.take(250))
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
        val playbackBuffer = max(AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), 9600)
        playback = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(playbackBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM).build().also { it.play() }
        val minBuffer = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) {
            _state.value = LiveUiState(status = "Microfone indisponível")
            recording.set(false)
            return
        }
        try {
            @Suppress("MissingPermission")
            val input = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuffer * 2, 4096))
            recorder = input
            input.startRecording()
            captureJob = io.launch {
                val buffer = ByteArray(2048)
                while (recording.get()) {
                    val count = input.read(buffer, 0, buffer.size)
                    if (count > 0 && recording.get()) {
                        val encoded = Base64.encodeToString(buffer.copyOf(count), Base64.NO_WRAP)
                        val payload = JSONObject().put("realtimeInput", JSONObject().put("audio", JSONObject()
                            .put("mimeType", "audio/pcm;rate=16000")
                            .put("data", encoded)))
                        socket?.send(payload.toString())
                    }
                }
            }
        } catch (_: Exception) {
            _state.value = LiveUiState(status = "Falha ao iniciar microfone")
            haltAudio()
        }
    }

    private fun haltAudio() {
        recording.set(false)
        try {recorder?.stop()} catch (_: Exception) { }
        captureJob?.cancel()
        try {recorder?.release()} catch (_: Exception) { }
        recorder = null
        try {playback?.stop()} catch (_: Exception) { }
        try {playback?.release()} catch (_: Exception) { }
        playback = null
    }

    fun disconnect() {
        recording.set(false)
        socket?.close(1000, "Usuário encerrou a sessão")
        socket = null
        haltAudio()
        _state.value = LiveUiState(status = "Desconectado")
    }

    fun close() {disconnect();incomingAudio.close();io.cancel();http.dispatcher.executorService.shutdown()}
}

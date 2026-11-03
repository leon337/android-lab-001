package br.com.laboratorio.android.live

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Phase { IDLE, CONNECTING, LIVE, ERROR }

data class ChatLine(
    val fromUser: Boolean,
    val text: String,
    val closed: Boolean = false,
)

data class UiState(
    val phase: Phase = Phase.IDLE,
    val lines: List<ChatLine> = emptyList(),
    val status: String = "Pronto. Toque em Iniciar conversa.",
    val muted: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class LiveViewModel(app: Application) : AndroidViewModel(app), GeminiLiveClient.Listener {
    private val preferences = app.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
    private val apiKeyStore = ApiKeyStore(app.applicationContext)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LiveSettings> = _settings.asStateFlow()

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val client = GeminiLiveClient(app.applicationContext, this)

    private fun loadSettings(): LiveSettings = LiveSettings(
        apiKey = apiKeyStore.load().orEmpty(),
        model = preferences.getString(KEY_MODEL, ModelCatalog.DEFAULT_MODEL)
            ?: ModelCatalog.DEFAULT_MODEL,
        voice = preferences.getString(KEY_VOICE, "Puck") ?: "Puck",
        thinkingLevel = preferences.getString(KEY_THINKING, "low") ?: "low",
        systemPrompt = preferences.getString(KEY_PROMPT, DEFAULT_SYSTEM_PROMPT)
            ?: DEFAULT_SYSTEM_PROMPT,
        speaker = preferences.getBoolean(KEY_SPEAKER, true),
    )

    fun saveSettings(value: LiveSettings) {
        val normalized = value.copy(apiKey = value.apiKey.trim())
        if (normalized.apiKey.isBlank()) {
            apiKeyStore.clear()
        } else {
            apiKeyStore.save(normalized.apiKey)
        }

        preferences.edit()
            .putString(KEY_MODEL, normalized.model)
            .putString(KEY_VOICE, normalized.voice)
            .putString(KEY_THINKING, normalized.thinkingLevel)
            .putString(KEY_PROMPT, normalized.systemPrompt)
            .putBoolean(KEY_SPEAKER, normalized.speaker)
            .apply()

        _settings.value = normalized
        _ui.update { it.copy(error = null, notice = null) }
    }

    fun clearApiKey() {
        apiKeyStore.clear()
        _settings.update { it.copy(apiKey = "") }
        stop()
    }

    fun start() {
        val selected = _settings.value
        if (selected.apiKey.isBlank()) {
            reportError("Configure a chave API do Gemini nas Configurações.")
            return
        }
        if (_ui.value.phase == Phase.LIVE || _ui.value.phase == Phase.CONNECTING) return

        _ui.value = UiState(
            phase = Phase.CONNECTING,
            status = "Preparando conexão segura…",
        )
        client.start(selected)
    }

    fun stop() {
        client.stop()
        _ui.update {
            it.copy(
                phase = Phase.IDLE,
                status = "Desconectado",
                muted = false,
                notice = null,
            )
        }
    }

    fun toggleMute() {
        val next = !_ui.value.muted
        client.setMuted(next)
        _ui.update { it.copy(muted = next) }
    }

    fun reportError(message: String) {
        _ui.update { it.copy(error = message) }
    }

    fun clearConversation() {
        _ui.update { it.copy(lines = emptyList(), error = null, notice = null) }
    }

    override fun onStatus(message: String) {
        _ui.update { it.copy(status = message) }
    }

    override fun onReady() {
        _ui.update {
            it.copy(
                phase = Phase.LIVE,
                status = "Ao vivo — pode falar",
                error = null,
                notice = null,
            )
        }
    }

    override fun onUserText(text: String) = appendText(fromUser = true, text = text)

    override fun onModelText(text: String) = appendText(fromUser = false, text = text)

    override fun onTurnComplete() = closeLastLine()

    override fun onInterrupted() = closeLastLine()

    override fun onInteractionStatus(status: String) {
        when (status.uppercase()) {
            "IN_PROGRESS" -> _ui.update {
                it.copy(status = "Gemini está raciocinando…")
            }
            "IDLE" -> _ui.update {
                it.copy(status = "Ao vivo — pode falar")
            }
        }
    }

    override fun onNotice(message: String) {
        _ui.update { it.copy(notice = message) }
    }

    override fun onSessionEnded(message: String) {
        _ui.update {
            it.copy(
                phase = Phase.IDLE,
                status = message,
                muted = false,
            )
        }
    }

    override fun onError(message: String) {
        _ui.update {
            it.copy(
                phase = Phase.ERROR,
                status = "Conexão encerrada",
                error = message,
                muted = false,
            )
        }
    }

    private fun appendText(fromUser: Boolean, text: String) {
        _ui.update { state ->
            val last = state.lines.lastOrNull()
            if (last != null && last.fromUser == fromUser && !last.closed) {
                state.copy(lines = state.lines.dropLast(1) + last.copy(text = last.text + text))
            } else {
                state.copy(lines = state.lines + ChatLine(fromUser = fromUser, text = text))
            }
        }
    }

    private fun closeLastLine() {
        _ui.update { state ->
            val last = state.lines.lastOrNull() ?: return@update state
            if (last.closed) state
            else state.copy(lines = state.lines.dropLast(1) + last.copy(closed = true))
        }
    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }

    private companion object {
        const val SETTINGS_PREFERENCES = "gemini_live_preferences"
        const val KEY_MODEL = "model"
        const val KEY_VOICE = "voice"
        const val KEY_THINKING = "thinking_level"
        const val KEY_PROMPT = "system_prompt"
        const val KEY_SPEAKER = "speaker"
    }
}

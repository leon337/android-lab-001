package br.com.laboratorio.android.live

const val DEFAULT_SYSTEM_PROMPT =
    "Você é um assistente de voz objetivo e natural. Responda sempre em português do Brasil, " +
        "com frases claras e curtas. Converse como uma pessoa, sem mencionar que está lendo transcrições."

data class LiveSettings(
    val apiKey: String = "",
    val model: String = ModelCatalog.DEFAULT_MODEL,
    val voice: String = "Puck",
    val thinkingLevel: String = "low",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val speaker: Boolean = true,
)

object ModelCatalog {
    const val DEFAULT_MODEL = "gemini-3.8-live"
    const val EXTENDED_THINKING_MODEL = "gemini-3.8-live-extended-thinking"

    data class ModelOption(
        val id: String,
        val label: String,
        val description: String,
        val supportsThinking: Boolean,
    )

    val models = listOf(
        ModelOption(
            id = DEFAULT_MODEL,
            label = "Gemini 3.8 Live (baixa latência)",
            description = "Conversação natural e respostas rápidas.",
            supportsThinking = false,
        ),
        ModelOption(
            id = EXTENDED_THINKING_MODEL,
            label = "Gemini 3.8 Live Extended Thinking",
            description = "Raciocínio em segundo plano para tarefas complexas.",
            supportsThinking = true,
        ),
    )

    val voices = listOf("Puck", "Kore", "Charon", "Fenrir", "Aoede", "Leda", "Orus", "Zephyr")
    val thinkingLevels = listOf("low", "medium", "high")

    fun supportsThinking(model: String): Boolean =
        models.firstOrNull { it.id == model }?.supportsThinking == true

    fun labelFor(model: String): String =
        models.firstOrNull { it.id == model }?.label ?: model
}

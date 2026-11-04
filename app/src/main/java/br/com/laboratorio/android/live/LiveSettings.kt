package br.com.laboratorio.android.live

import br.com.laboratorio.android.personality.BehaviorCatalog
import br.com.laboratorio.android.personality.PersonalityCatalog
import br.com.laboratorio.android.personality.composeAssistantSystemInstruction

const val DEFAULT_SYSTEM_PROMPT =
    "Você é um assistente de voz objetivo e natural. Responda sempre em português do Brasil, " +
        "com frases claras e curtas. Converse como uma pessoa, sem mencionar que está lendo transcrições."

data class LiveSettings(
    val apiKey: String = "",
    val model: String = ModelCatalog.DEFAULT_MODEL,
    val voice: String = "Puck",
    val thinkingLevel: String = "low",
    val systemPrompt: String = "",
    val speaker: Boolean = true,
    val personalityId: String = PersonalityCatalog.DEFAULT_ID,
    val verbosity: String = BehaviorCatalog.DEFAULT_VERBOSITY,
    val tone: String = BehaviorCatalog.DEFAULT_TONE,
    val initiative: String = BehaviorCatalog.DEFAULT_INITIATIVE,
    val approach: String = BehaviorCatalog.DEFAULT_APPROACH,
) {
    fun effectiveSystemInstruction(): String = composeAssistantSystemInstruction(
        personalityId = personalityId,
        verbosity = verbosity,
        tone = tone,
        initiative = initiative,
        approach = approach,
        customInstructions = systemPrompt,
    )
}

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

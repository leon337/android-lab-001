package br.com.laboratorio.android.live

import org.json.JSONObject

internal data class GeminiServerSignals(
    val interactionStatus: String,
    val goAway: Boolean,
)

internal fun parseGeminiServerSignals(packet: JSONObject): GeminiServerSignals {
    val serverContent = packet.optJSONObject("serverContent")
    val interactionStatus = packet.optString("interactionStatus")
        .ifBlank { serverContent?.optString("interactionStatus", "").orEmpty() }

    return GeminiServerSignals(
        interactionStatus = interactionStatus,
        goAway = packet.has("goAway"),
    )
}

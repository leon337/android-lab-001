package br.com.laboratorio.android.live

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveProtocolInstrumentedTest {
    @Test
    fun parsesTopLevelInteractionStatusWithoutServerContent() {
        val signals = parseGeminiServerSignals(
            JSONObject().put("interactionStatus", "IN_PROGRESS"),
        )

        assertEquals("IN_PROGRESS", signals.interactionStatus)
        assertFalse(signals.goAway)
    }

    @Test
    fun fallsBackToNestedInteractionStatus() {
        val packet = JSONObject().put(
            "serverContent",
            JSONObject().put("interactionStatus", "IDLE"),
        )

        assertEquals("IDLE", parseGeminiServerSignals(packet).interactionStatus)
    }

    @Test
    fun prefersNonBlankTopLevelStatusWhenBothArePresent() {
        val packet = JSONObject()
            .put("interactionStatus", "IN_PROGRESS")
            .put("serverContent", JSONObject().put("interactionStatus", "IDLE"))

        assertEquals("IN_PROGRESS", parseGeminiServerSignals(packet).interactionStatus)
    }

    @Test
    fun detectsGoAwayWithoutServerContent() {
        val packet = JSONObject().put("goAway", JSONObject().put("timeLeft", "1s"))
        val signals = parseGeminiServerSignals(packet)

        assertTrue(signals.goAway)
        assertEquals("", signals.interactionStatus)
    }
}

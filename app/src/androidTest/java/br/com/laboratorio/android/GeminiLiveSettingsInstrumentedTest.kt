package br.com.laboratorio.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class GeminiLiveSettingsInstrumentedTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun telaInicialExibeConfiguracaoDeChaveEModelosLive38() {
        rule.onNodeWithText("Chave de API do Gemini").assertIsDisplayed()
        rule.onNodeWithTag("api-key-input").assertIsDisplayed()
        rule.onNodeWithText("Gemini 3.8 Live (baixa latência)").assertIsDisplayed()
        rule.onNodeWithText("Gemini 3.8 Live Extended Thinking").assertIsDisplayed()
        rule.onNodeWithTag("settings-save").assertIsNotEnabled()
        rule.onNodeWithText("Personalidade do assistente").fetchSemanticsNode()
        rule.onNodeWithText("Professor paciente").fetchSemanticsNode()
        rule.onNodeWithTag("personality-developer").fetchSemanticsNode()
        rule.onNodeWithText("Comportamento").fetchSemanticsNode()
        rule.onNodeWithTag("open-updates").fetchSemanticsNode()
    }
}

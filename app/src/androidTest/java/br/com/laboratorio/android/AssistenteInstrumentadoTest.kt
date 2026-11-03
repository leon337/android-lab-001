package br.com.laboratorio.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class AssistenteInstrumentadoTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun criarResumoMockEAprovar() {
        rule.onNodeWithText("SIMULAÇÃO LOCAL — nenhuma IA real conectada").assertIsDisplayed()
        rule.onNodeWithTag("nota-input").performTextInput("Aprendi Kotlin e Compose para construir aplicativos.")
        rule.onNodeWithText("Gerar resumo (Mock)").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithTag("resumo-resultado").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("resumo-resultado").assertIsDisplayed()
        rule.onNodeWithText("Aprovar resumo").performScrollTo().performClick()
        rule.onNodeWithText("Resumo aprovado").assertIsDisplayed()
    }

    @Test
    fun descartarResumoVoltaAoEstadoSemSugestao() {
        rule.onNodeWithTag("nota-input").performTextInput("Testando o descarte de um resumo.")
        rule.onNodeWithText("Gerar resumo (Mock)").performClick()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithTag("resumo-resultado").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Descartar resumo").performScrollTo().performClick()
        rule.onNodeWithTag("resumo-resultado").assertDoesNotExist()
    }
}

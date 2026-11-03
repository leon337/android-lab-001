package br.com.laboratorio.android.ai

import kotlinx.coroutines.delay

/**
 * Simulador deterministico. NAO consulta modelos de inteligencia artificial.
 */
class MockAIRepository : AIRepository {
    override suspend fun resumir(nota: String): String {
        delay(180)
        val texto = nota.trim().replace(Regex("\\s+"), " ")
        require(texto.isNotBlank()) { "Escreva uma anotação antes de gerar o resumo." }
        val corte = texto.take(125)
        val sufixo = if (texto.length > 125) "…" else ""
        return "Resumo simulado: $corte$sufixo"
    }
}

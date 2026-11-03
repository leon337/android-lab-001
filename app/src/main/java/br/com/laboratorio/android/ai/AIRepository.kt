package br.com.laboratorio.android.ai

/**
 * Abstracao para obter uma sugestao de resumo.
 * A implementacao pode ser local (Mock) ou remota (fase futura).
 */
interface AIRepository {
    suspend fun resumir(nota: String): String
}

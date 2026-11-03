package br.com.laboratorio.android.personality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPersonalityInstrumentedTest {
    @Test
    fun catalogoDisponibilizaSeisPersonalidades() {
        assertEquals(6, PersonalityCatalog.options.size)
        assertEquals("balanced", PersonalityCatalog.DEFAULT_ID)
    }

    @Test
    fun instrucoesCombinamPersonalidadeEPreferenciasComportamentais() {
        val prompt = composeAssistantSystemInstruction(
            personalityId = "developer",
            verbosity = "concise",
            tone = "formal",
            initiative = "proactive",
            approach = "critical",
            customInstructions = "Sempre apresente riscos de segurança.",
        )

        assertTrue(prompt.contains("engenheiro de software sênior"))
        assertTrue(prompt.contains("Use respostas curtas"))
        assertTrue(prompt.contains("linguagem formal"))
        assertTrue(prompt.contains("Antecipe riscos"))
        assertTrue(prompt.contains("contraexemplos") || prompt.contains("premissas frágeis"))
        assertTrue(prompt.contains("Sempre apresente riscos de segurança."))
    }

    @Test
    fun identificadorDesconhecidoUsaPerfilEquilibrado() {
        assertEquals(
            PersonalityCatalog.find(PersonalityCatalog.DEFAULT_ID),
            PersonalityCatalog.find("perfil-inexistente"),
        )
    }
}

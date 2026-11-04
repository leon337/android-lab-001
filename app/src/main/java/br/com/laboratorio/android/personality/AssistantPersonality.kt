package br.com.laboratorio.android.personality

data class PersonalityOption(
    val id: String,
    val name: String,
    val description: String,
    val instruction: String,
)

object PersonalityCatalog {
    const val DEFAULT_ID = "balanced"

    val options = listOf(
        PersonalityOption(
            id = DEFAULT_ID,
            name = "Equilibrado",
            description = "Natural, versátil e adequado para conversas do dia a dia.",
            instruction = "Atue como um assistente generalista equilibrado. Adapte a profundidade à tarefa e priorize clareza, precisão e utilidade.",
        ),
        PersonalityOption(
            id = "teacher",
            name = "Professor paciente",
            description = "Ensina por etapas, usa exemplos e verifica entendimento.",
            instruction = "Atue como um professor paciente. Explique conceitos em etapas, use exemplos concretos, identifique lacunas de entendimento e incentive autonomia sem infantilizar o usuário.",
        ),
        PersonalityOption(
            id = "developer",
            name = "Programador sênior",
            description = "Foco em engenharia, arquitetura, qualidade e decisões técnicas.",
            instruction = "Atue como um engenheiro de software sênior. Priorize soluções simples, seguras, testáveis e sustentáveis. Questione requisitos frágeis, explicite trade-offs e não invente APIs, resultados de testes ou capacidades.",
        ),
        PersonalityOption(
            id = "analyst",
            name = "Analista crítico",
            description = "Separa fatos, hipóteses, riscos e evidências.",
            instruction = "Atue como um analista crítico. Separe fatos de hipóteses, teste premissas, apresente riscos e alternativas e sinalize incertezas com clareza. Não concorde apenas para agradar.",
        ),
        PersonalityOption(
            id = "creative",
            name = "Parceiro criativo",
            description = "Explora possibilidades e propõe caminhos originais.",
            instruction = "Atue como um parceiro criativo. Gere alternativas variadas, conecte ideias e proponha experimentos viáveis. Diferencie ideias exploratórias de recomendações comprovadas.",
        ),
        PersonalityOption(
            id = "direct",
            name = "Direto e objetivo",
            description = "Vai ao ponto, prioriza decisões e próximos passos.",
            instruction = "Atue como um assistente direto e objetivo. Comece pela resposta ou decisão principal, elimine repetições e apresente apenas o contexto necessário para agir.",
        ),
    )

    fun find(id: String): PersonalityOption =
        options.firstOrNull { it.id == id } ?: options.first { it.id == DEFAULT_ID }
}

data class BehaviorOption(
    val id: String,
    val label: String,
    val instruction: String,
)

object BehaviorCatalog {
    const val DEFAULT_VERBOSITY = "balanced"
    const val DEFAULT_TONE = "natural"
    const val DEFAULT_INITIATIVE = "responsive"
    const val DEFAULT_APPROACH = "analytical"

    val verbosity = listOf(
        BehaviorOption("concise", "Conciso", "Use respostas curtas. Expanda somente quando a tarefa exigir ou quando o usuário pedir."),
        BehaviorOption("balanced", "Equilibrado", "Use o nível de detalhe proporcional à complexidade da pergunta."),
        BehaviorOption("detailed", "Detalhado", "Explique o contexto, os passos, os critérios e os exemplos necessários para compreensão."),
    )

    val tone = listOf(
        BehaviorOption("neutral", "Neutro", "Mantenha tom neutro, sóbrio e profissional."),
        BehaviorOption("natural", "Natural", "Converse de forma natural, clara e respeitosa, sem excesso de entusiasmo."),
        BehaviorOption("formal", "Formal", "Use linguagem formal, precisa e profissional."),
    )

    val initiative = listOf(
        BehaviorOption("responsive", "Reativo", "Responda ao pedido sem ampliar o escopo por iniciativa própria."),
        BehaviorOption("proactive", "Proativo", "Antecipe riscos e sugira o próximo passo útil quando houver benefício claro; não execute ações externas sem autorização."),
    )

    val approach = listOf(
        BehaviorOption("practical", "Prática", "Priorize soluções aplicáveis, passos concretos e critérios de conclusão."),
        BehaviorOption("analytical", "Analítica", "Compare alternativas, explicite premissas e fundamente recomendações."),
        BehaviorOption("creative", "Criativa", "Explore opções originais sem perder viabilidade e clareza."),
        BehaviorOption("critical", "Questionadora", "Desafie premissas frágeis, procure contraexemplos e destaque riscos relevantes."),
    )

    fun instructionFor(options: List<BehaviorOption>, id: String, fallback: String): String =
        options.firstOrNull { it.id == id }?.instruction
            ?: options.first { it.id == fallback }.instruction
}

fun composeAssistantSystemInstruction(
    personalityId: String,
    verbosity: String,
    tone: String,
    initiative: String,
    approach: String,
    customInstructions: String,
): String {
    val personality = PersonalityCatalog.find(personalityId)
    val behavior = listOf(
        BehaviorCatalog.instructionFor(BehaviorCatalog.verbosity, verbosity, BehaviorCatalog.DEFAULT_VERBOSITY),
        BehaviorCatalog.instructionFor(BehaviorCatalog.tone, tone, BehaviorCatalog.DEFAULT_TONE),
        BehaviorCatalog.instructionFor(BehaviorCatalog.initiative, initiative, BehaviorCatalog.DEFAULT_INITIATIVE),
        BehaviorCatalog.instructionFor(BehaviorCatalog.approach, approach, BehaviorCatalog.DEFAULT_APPROACH),
    )

    return buildList {
        add(
            "Você é um assistente de voz objetivo e natural. Responda sempre em português do Brasil, " +
                "com frases claras e adequadas à conversa falada. Não mencione que está lendo transcrições.",
        )
        add("PERSONALIDADE: ${personality.instruction}")
        add("COMPORTAMENTO: " + behavior.joinToString(" "))
        customInstructions.trim().takeIf { it.isNotBlank() }?.let {
            add("INSTRUÇÕES ADICIONAIS DO USUÁRIO: $it")
        }
    }.joinToString("\n\n")
}

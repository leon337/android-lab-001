# Experimento 003 — Fase A (Android + Mock AI + HITL)

## Contrato de fase
- `mission_id`: ANDROID-LAB-003
- `phase_id`: A
- `title`: Assistente de Anotações com IA simulada
- `objective`: construir tela Android para anotar, gerar sugestão simulada e aprovar/descartar com supervisão humana.
- `expected_outcome`: APK debug e APK instrumentado compilados; testes funcionais no emulador e evidências visuais pelo GitHub/MBC.
- `scope`: Kotlin/Compose, ViewModel, AIRepository, MockAIRepository, estado e erros, aprovação/descartes, regressão do contador, CI gratuito e documentação.
- `out_of_scope`: Firebase, Gemini real, credenciais, faturamento, conta nova na nuvem, backend OCI, Play Store, apps no telefone físico.
- `source_of_truth`: repositórios `leon337/android-lab-001` e `leon337/multiagent-collaboration-framework`.
- `acceptance_criteria`: 3 testes instrumentados passando (contador e duas ações de resumo), Mock visivelmente identificado, captura inicial/sugestão/aprovação sem API externa.
- `authorizations`: aprovação expressa da Fase A, GitHub Actions e MBC.
- `prohibitions`: não configurar Firebase nem credenciais até novo HUMAN_GATE.
- `risk_class`: A (laboratório educacional sem credenciais ou dados pessoais).
- `current_state`: EM_IMPLEMENTACAO.
- `cycle`: 1.
- `selected_agents`: coordenação MESTRE nesta conversa; sem alegação de agentes independentes ou runtime MCF persistente.
- `decision_authority`: LEANDRO para gates humanos; decisões operacionais restritas ao escopo aprovado.
- `human_escalation_triggers`: primeira chamada de IA real, criação de projeto Firebase, permissões ou cobrança, revisão final da interface.
- `phase_artifact_directory`: docs/EXPERIMENTO-003-FASE-A-RESULTADO.md e docs/evidencias/experimento-003/.

## Arquitetura
`Compose -> NotesViewModel (StateFlow, viewModelScope) -> AIRepository -> MockAIRepository`.

Substituir `MockAIRepository` por uma implementação Firebase será objeto de nova aprovação.
O Mock retorna texto previsível e nunca faz inferência nem usa rede. O aplicativo sinaliza isso.

## Critérios de teste
- Instrumentação Compose: gerar e aprovar, gerar e descartar, regressão do contador.
- Testes não devem consumir API nem exigir token.
- Capturas no emulador para demonstrar interface e proposta HITL.
- Registro de falhas e correções com links de workflow/commit.

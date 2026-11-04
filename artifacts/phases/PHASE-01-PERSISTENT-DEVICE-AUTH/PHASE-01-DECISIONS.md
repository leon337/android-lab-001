# Decisões — PHASE-01

## D1 — Credencial stateless assinada

Usar HMAC-SHA256 derivado do segredo já existente LAB_PAIRING_CODE com separação de domínio. Evita banco novo e filesystem persistente no Render Free. Token expira em 365 dias e é renovado nos últimos 30 dias.

## D2 — Armazenamento Android

Usar Android Keystore para manter chave AES e AES-GCM para cifrar o token em SharedPreferences. Não salvar o código de ativação. O UUID da instalação não é segredo.

## D3 — Sem novas variáveis de ambiente

Não adicionar segredo de assinatura. A derivação HMAC usa LAB_PAIRING_CODE já existente no servidor. GEMINI_API_KEY continua exclusivamente no Render.

## D4 — Compatibilidade legada

Manter aceitação do código antigo em /live/token durante a migração de APKs. Remover essa compatibilidade após a adoção do novo fluxo e revisão de segurança.

## D5 — Limites de revogação

Sem banco, não há revogação individual persistente. Rotacionar LAB_PAIRING_CODE invalida todas as credenciais. Documentar isso como limite de MVP privado.

## D6 — Sem deploy ou merge nesta fase

A autorização recebida cobre a implementação da arquitetura. Deploy do serviço Render, mudança de configuração, merge em main e release são ações externas separadas. O PR permanece draft.

## D7 — Governança MCF

O repositório MCF foi consultado no estado atual. Não havia runtime de agentes independentes disponível nesta sessão; nenhuma participação independente foi simulada ou atribuída. O gate de revisão independente permanece pendente.

## D8 — Versão do WebSocket para token efêmero

A implementação inicial usava v1beta no WebSocket. A consulta ao SDK oficial Google confirmou que tokens auth_tokens/... usam BidiGenerateContentConstrained e conexão v1alpha. O Android foi corrigido no commit fdd8b8c. A REST de criação do token permanece em v1beta/auth_tokens. O teste real de WebSocket continua pendente.

## D9 — Sem deploy ou teste real nesta etapa

A correção do endpoint foi validada por build e testes automatizados; o backend novo ainda não foi implantado. Não executar deploy, alterar variáveis ou instalar o novo APK no telefone antes de novo HUMAN_GATE.

## D10 — Revisão independente pendente

O runtime local-agent-runtime exigido pela skill MCF-EXECUTE-LOCAL-TEAM não está disponível. Nenhuma atuação de Miriam, Tiago, Rafael, Ricardo, Júlia, Beatriz, Augusto, Emily, Léo, Gabriel, Carmem ou Renato será simulada. O parecer de MESTRE é preliminar; auditoria independente e gate de Léo permanecem pendentes.

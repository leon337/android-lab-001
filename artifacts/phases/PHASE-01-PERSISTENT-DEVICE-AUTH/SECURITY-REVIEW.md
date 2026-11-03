# Revisão de segurança — PHASE-01

**Tipo:** revisão técnica preliminar feita por MESTRE; não é auditoria independente.
**Veredito:** SELF_REVIEW_PASS_WITH_RESERVATIONS.
**Auditoria independente Emily / gate Léo:** pendentes porque o runtime multiagente local não está disponível nesta sessão.

## Ativos e fluxo de dados

- GEMINI_API_KEY: segredo permanente, mantido somente no backend Render.
- LAB_PAIRING_CODE: segredo compartilhado de bootstrap, enviado somente no enrollment HTTPS e usado para derivar a chave HMAC.
- Credencial do dispositivo: bearer token auto-contido, assinado HMAC-SHA256, validade de 365 dias.
- Chave AES: criada e mantida pelo Android Keystore.
- Credencial do dispositivo em repouso: cifrada com AES-GCM em SharedPreferences.
- Token Gemini efêmero: emitido a cada sessão, usado no WebSocket e não persistido em claro.
- Áudio: microfone Android envia PCM 16 kHz ao Google Live API; resposta PCM 24 kHz é reproduzida localmente.

## Controles verificados no código

1. Comparação do código de pareamento por timingSafeEqual.
2. Derivação HMAC com separação de domínio.
3. Validação de assinatura, emissor, audiência, versão, UUID, iat e exp.
4. Respostas sem cache (Cache-Control: no-store, Pragma: no-cache).
5. Armazenamento AES-GCM com chave Android Keystore.
6. Código de pareamento não é salvo pelo app.
7. Chave Gemini não é compilada no APK.
8. Logs não devem incluir tokens ou corpo bruto do provedor.
9. A compatibilidade legada em /live/token é temporária e documentada.
10. APK do commit fdd8b8c passou pela varredura de strings sem encontrar padrões de chave Google, LAB_PAIRING_CODE ou GEMINI_API_KEY.

## Achados e riscos residuais

| ID | Severidade | Achado | Tratamento |
|---|---|---|---|
| SEC-01 | Média | Credencial do dispositivo é bearer e pode ser reutilizada por quem a copiar até expirar. | Aceitável somente para laboratório privado; Keystore reduz exposição em repouso. |
| SEC-02 | Média | Não existe revogação individual persistente. | Rotacionar LAB_PAIRING_CODE invalida todas as credenciais; registrar como limite do MVP. |
| SEC-03 | Média | Rate limit de /live/token é em memória e reinicia com o processo. | Proteção complementar, não quota durável/distribuída. |
| SEC-04 | Média | /auth/enroll não possui rate limit por IP nesta versão. | Exigir código de bootstrap gerado aleatoriamente com alta entropia; adicionar limitação durável antes de distribuição pública. |
| SEC-05 | Média | A validade mínima de 32 caracteres do segredo não prova entropia suficiente. | Antes do deploy, confirmar que o segredo existente é aleatório e não uma frase escolhida manualmente; não ler nem registrar seu valor. |
| SEC-06 | Média | Credencial é vinculada logicamente ao UUID, não a hardware/atestado do dispositivo. | Não afirmar proteção anti-clonagem física. |
| SEC-07 | Média | Endpoint WebSocket de tokens efêmeros usa v1alpha conforme o SDK oficial. | Interface experimental/preview; validar ponta a ponta antes de depender do serviço. |
| SEC-08 | Alta para aceite funcional | Ainda não existe evidência de sessão WebSocket Gemini real com o APK corrigido. | Não declarar integração funcional até teste autorizado após deploy. |
| SEC-09 | Baixa | A descrição atual do PR #2 contém texto legado que não reflete todos os resultados recentes. | Atualização do corpo do PR é uma escrita pública e fica pendente de gate interno. |

## Protocolo Gemini verificado

- O backend cria o token efêmero pela API REST v1beta/auth_tokens.
- O SDK oficial Google identifica tokens auth_tokens/..., seleciona BidiGenerateContentConstrained, envia Authorization: Token ... e informa que a conexão efêmera é suportada em v1alpha.
- O Android foi corrigido para usar v1alpha no WebSocket. Essa correção foi compilada e testada no emulador; não prova, por si só, uma sessão real com o Google.
- Modelo: gemini-3.8-live-extended-thinking; thinking level: LOW.
- A API de tokens efêmeros está em preview/experimental. A compatibilidade deve ser reavaliada antes de qualquer distribuição pública.

## Condições para aceitar o risco no laboratório

1. Uso privado, por um único operador/dispositivo controlado.
2. LAB_PAIRING_CODE deve ser aleatório, forte e nunca embutido no APK.
3. GEMINI_API_KEY permanece somente no Render.
4. Não publicar o APK nem abrir enrollment a múltiplos usuários.
5. Não ativar billing ou plano pago sem gate humano específico.
6. Testar enrollment, refresh, token efêmero e WebSocket real após deploy autorizado.
7. Planejar remoção da rota legada após migração confirmada.

## Limite do parecer

Este documento é uma revisão preliminar de MESTRE baseada em código, testes e documentação oficial. Não substitui a revisão de Ricardo/Júlia, a auditoria independente de Emily nem a decisão de Léo. O status da fase permanece EM_REVISAO.

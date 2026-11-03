# Android Lab — Gemini Live Extended Thinking

**Modelo:** gemini-3.8-live-extended-thinking, thinking level LOW.
**Estratégia:** microfone Android em PCM 16 kHz → Live WebSocket → áudio do modelo PCM 24 kHz → alto-falante Android.
**Backend:** Node.js no Render, com chave Gemini exclusivamente no servidor e tokens efêmeros de uso único.

## Autenticação do dispositivo

O primeiro uso exige um pareamento único por código de ativação. O broker troca esse código por uma credencial assinada HMAC-SHA256, com validade de 365 dias. O Android cifra a credencial com AES-GCM e guarda a chave criptográfica no Android Keystore. Quando restarem 30 dias, o aplicativo renova a credencial automaticamente.

O código de ativação não é persistido no telefone. A chave Gemini permanente não é enviada ao APK. A cada nova conversa, o aplicativo solicita ao broker um token efêmero Gemini de uso único.

A credencial é stateless e vinculada logicamente ao UUID da instalação. Esta versão não oferece revogação individual persistente; a rotação de LAB_PAIRING_CODE invalida todas as credenciais emitidas com a chave anterior. O código antigo continua temporariamente aceito em /live/token para compatibilidade com APKs anteriores.

## Segurança e custo

- Não embutir GEMINI_API_KEY, LAB_PAIRING_CODE ou token de dispositivo no APK.
- O segredo LAB_PAIRING_CODE é usado somente no enrollment inicial e para derivar a chave de assinatura no backend.
- O token de dispositivo é bearer; o armazenamento criptografado reduz exposição em repouso, mas não equivale a autenticação multiusuário com revogação individual.
- O rate limit atual é em memória e reinicia após reinicialização do processo.
- O uso da API Gemini depende das cotas e políticas da conta Google; não habilitar billing ou serviços pagos sem novo HUMAN_GATE.
- O conteúdo de voz pode ser processado pelo provedor. Usar conteúdo fictício durante testes.

## Critérios de aceitação

1. /health responde 200 sem credencial.
2. /auth/enroll exige o código correto e emite credencial assinada.
3. /auth/refresh renova credencial válida e rejeita token inválido/expirado.
4. /live/token exige credencial válida e retorna token efêmero.
5. Android guarda o token criptografado no Keystore e não salva o código de ativação.
6. A interface solicita o código somente no primeiro pareamento.
7. Build APK e testes instrumentados passam.
8. Sessão Gemini real só pode ser declarada validada depois de deploy autorizado do backend e teste ponta a ponta.

## Estado de publicação

A integração permanece em PR draft. Esta fase não altera variáveis do Render, não faz deploy, não faz merge em main e não publica release. Esses limites exigem gate separado.

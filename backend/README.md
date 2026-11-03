# Broker de tokens temporários Gemini Live

Servidor Node.js 20+ para uso experimental. Não armazena conteúdo de conversas.

- `GET /health`: somente estado do serviço, sem credencial.
- `POST /live/token`: recebe `Authorization: Bearer <LAB_PAIRING_CODE>` via HTTPS, pede ao Google um token efêmero de uso único, vinculado ao modelo `gemini-3.8-live-extended-thinking` e à modalidade AUDIO. Somente o token temporário é retornado ao Android.

Configure no serviço de hospedagem os segredos **LAB_PAIRING_CODE** (pelo menos 32 caracteres aleatórios) e **GEMINI_API_KEY** (chave permanente do projeto Google AI Studio). Nunca publique valores em repositório, log, BuildConfig, APK ou documentação.

O pareamento por código é apropriado **somente para laboratório de um usuário**. Para publicação e múltiplos usuários, substitua o código por autenticação real e limites persistentes por usuário/dispositivo. O rate limit em memória (6 sessões/hora por IP) é proteção complementar e reinicia em novos processos.

Testes locais sem credenciais reais: `cd backend && npm test`.

Para o Render Free, configurar Build `npm --prefix backend install --ignore-scripts`, Start `node backend/server.mjs`, região `virginia`, plano `free`. A aplicação pode levar cerca de um minuto para acordar após 15 min de inatividade.

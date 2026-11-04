# Broker de autenticação e tokens temporários Gemini Live

Servidor Node.js 20+ para o laboratório privado Android.

## Rotas

- GET /health: estado do serviço, sem credencial.
- POST /auth/enroll: recebe o código compartilhado de ativação uma única vez e o UUID da instalação; emite uma credencial de dispositivo assinada por HMAC-SHA256.
- POST /auth/refresh: renova uma credencial de dispositivo válida.
- POST /live/token: valida a credencial do dispositivo e solicita ao Google um token efêmero de uso único.

A credencial de dispositivo expira em 365 dias. O aplicativo tenta renová-la automaticamente quando restam 30 dias. O token fica criptografado no Android Keystore usando AES-GCM. O código de ativação não é salvo no telefone.

## Segredos e armazenamento

Configure somente no serviço de hospedagem:

- LAB_PAIRING_CODE: código aleatório forte já usado no laboratório (mínimo de 32 caracteres).
- GEMINI_API_KEY: chave permanente do projeto Google AI Studio.

A chave de assinatura das credenciais é derivada de LAB_PAIRING_CODE com separação de domínio. Nenhuma variável de ambiente nova é necessária. A chave Gemini e o código de ativação nunca devem aparecer em repositório, log, BuildConfig ou APK.

## Limites de segurança

- O token de dispositivo é uma credencial bearer auto-contida. O backend verifica assinatura, emissor, audiência, UUID, emissão e expiração.
- O Render Free não fornece persistência local garantida; por isso a autenticação do MVP não depende de banco ou arquivo no servidor.
- Não há revogação individual persistente de dispositivos nesta versão. Rotacionar LAB_PAIRING_CODE invalida todas as credenciais emitidas com a chave anterior.
- O rate limit de seis sessões por hora e por dispositivo é mantido em memória e reinicia quando o processo do serviço reinicia; é uma proteção complementar, não uma quota global durável.
- A rota /live/token mantém compatibilidade temporária com o código antigo para APKs anteriores. Essa compatibilidade deve ser removida após a migração confirmada.
- Este desenho é adequado ao laboratório privado de um usuário. Para publicação ou múltiplos usuários, adotar autenticação individual e revogação persistente.

## Testes

Execute npm test dentro de backend. Os testes usam uma chave fictícia e um provedor Google simulado; não precisam de credenciais reais.

## Render

Build: npm --prefix backend install --ignore-scripts
Start: node backend/server.mjs
Região: virginia
Plano: free

O código desta fase ainda não foi implantado no Render. Deploy e alteração de configuração do serviço exigem autorização separada.

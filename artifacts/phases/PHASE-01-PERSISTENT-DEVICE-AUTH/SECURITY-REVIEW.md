# Revisão de segurança — autenticação persistente do dispositivo

## Dados e credenciais

- Código de ativação: segredo de bootstrap, recebido somente pelo endpoint de enrollment via HTTPS.
- Credencial do dispositivo: bearer token assinado HMAC-SHA256, auto-contido, validade de 365 dias.
- Token Gemini: credencial efêmera de uso único, emitida pelo Google e usada somente na abertura da sessão WebSocket.
- Chave Gemini permanente: permanece no ambiente do backend.

## Controles implementados

1. Comparação do código inicial com timingSafeEqual.
2. Chave HMAC derivada do segredo existente com separação de domínio.
3. Validação de assinatura, emissor, audiência, versão, UUID, iat e exp.
4. Respostas HTTP sem cache (Cache-Control: no-store e Pragma: no-cache).
5. Token Android cifrado com AES-GCM e chave guardada no Android Keystore.
6. Código inicial não persistido no app.
7. Logs não incluem credenciais nem corpo bruto do provedor.
8. Compatibilidade legada temporária documentada e limitada por rate limit em memória.

## Riscos residuais

- A credencial é bearer: cópia do token permite uso até expirar ou o segredo de assinatura ser rotacionado.
- Não há revogação individual persistente sem banco/estado de servidor.
- Rate limit em memória não é distribuído nem durável e reinicia com o processo.
- O código de bootstrap compartilhado ainda pode cadastrar outro dispositivo enquanto permanecer válido.
- A sessão Live e o processamento de áudio dependem das cotas e políticas do Google.

## Veredito

PASS_WITH_RESERVATIONS para laboratório privado de um usuário, condicionado a manter o APK privado, proteger os segredos no Render e não declarar autenticação pública multiusuário. Para distribuição pública, exigir autenticação individual, revogação persistente, rate limit durável e nova revisão.

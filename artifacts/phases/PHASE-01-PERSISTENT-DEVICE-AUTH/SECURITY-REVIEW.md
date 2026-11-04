# Revisão de segurança — Gemini Live Android (PR #2)

**Escopo:** MVP Android de conversação por voz e cliente Gemini Live.
**Tipo:** self-review técnico de MESTRE, baseado no código e nas evidências disponíveis.
**Veredito:** SELF_REVIEW_PASS_WITH_RESERVATIONS.
**Auditoria independente:** não executada; este documento não é aprovação humana de merge.

## 1. Arquitetura efetivamente usada pelo APK

O APK usa conexão direta com a Gemini Live API. O broker Node/Render presente em backend/server.mjs é experimental e não é chamado pelo aplicativo.

1. LEANDRO cadastra sua própria chave API na tela de configurações.
2. ApiKeyStore cifra a chave com AES-GCM e guarda o ciphertext em SharedPreferences privadas.
3. A chave AES é mantida pelo Android Keystore.
4. Ao iniciar uma sessão, o app envia a chave permanente por HTTPS ao endpoint Google v1beta/auth_tokens, no header x-goog-api-key.
5. O pedido configura um token de uso único, com janela de 60 segundos para iniciar a sessão e expiração de 30 minutos.
6. O app abre WebSocket WSS para BidiGenerateContentConstrained usando o token efêmero no header Authorization: Token. A chave API permanente não é enviada no WebSocket.
7. O áudio PCM 16 kHz é transmitido ao Google e o áudio PCM 24 kHz é reproduzido localmente.
8. Transcrições e estado da conversa permanecem em memória durante a sessão; não existe histórico persistente nesta fase.

A documentação específica de tokens efêmeros do Google aceita o token no header Authorization: Token ou no parâmetro access_token. O cliente usa o header para evitar colocar a credencial na URL.

## 2. Controles observados

- Chave API cifrada com AES-GCM; chave AES no Android Keystore.
- Nenhuma chave API fixa no APK, BuildConfig ou repositório.
- HTTPS para emissão do token e WSS para a sessão Live.
- Token efêmero de uso único e curta duração.
- Token enviado no header Authorization: Token, não na query string.
- Cliente HTTP de autenticação separado do WebSocket.
- Timeouts finitos para autenticação: conexão 10 s, leitura 15 s, escrita 10 s e chamada total 25 s.
- Cancelamento explícito da chamada de token ao encerrar ou falhar a sessão.
- Ausência de interceptor de logging HTTP; erros são sanitizados antes de chegar à interface.
- Buffer de áudio limitado a 32 chunks de até 64 KiB; sob pressão, descarta o áudio mais antigo.
- Android backup desabilitado para os dados do aplicativo.

## 3. Evidência funcional e CI

Há evidência manual histórica do APK 1.0.0 instalado no OPPO RMX3491 / Android 13: emissão de token, abertura de WebSocket, transcrição de entrada, resposta textual, mute/unmute e persistência da chave após reinício. Essa evidência antecede as alterações atuais e não comprova uma sessão Live no HEAD atual do PR #2.

A execução GitHub Actions 41749388734 concluiu com sucesso:
- testes unitários;
- compilação do APK debug e do pacote de testes instrumentados;
- testes instrumentados no emulador;
- smoke da interface, com confirmação do campo de API Key e dos dois modelos Live 3.8.

O CI não usa uma API Key real e não inicia uma sessão Gemini Live real. Portanto, CI verde comprova build, testes e interface, não a integração ponta a ponta com a API do Google.

## 4. Achados e riscos residuais

| ID | Severidade | Achado | Controle / decisão |
|---|---|---|---|
| SEC-01 | Média | A chave API existe no cliente e pode ser extraída de um dispositivo comprometido, com root ou instrumentação em runtime. | AES-GCM/Keystore protege em repouso, mas não torna um segredo de cliente impossível de extrair. Uso privado; não distribuir APK com chave cadastrada. |
| SEC-02 | Média | A restrição da chave Google ao package Android e ao certificado de assinatura permanente ainda depende da migração para a assinatura persistente. | Após a migração autorizada, restringir a chave ao package br.com.laboratorio.android e ao certificado de release. |
| SEC-03 | Média | Um token efêmero pode ser reutilizado se for capturado durante sua janela válida, embora seja de uso único e curta duração. | Transporte no header Authorization: Token, WSS, ausência de logging interceptor e sanitização de erros. Não registrar headers de autenticação. |
| SEC-04 | Média | O cliente conecta diretamente à API e não possui rate limit central próprio. | A cota depende do projeto Google; reavaliar limites e restrições antes de distribuição. |
| SEC-05 | Média | Gemini Live e seus modelos usam uma superfície de API sujeita a mudanças de compatibilidade. | Validar sessão real em cada release; não tratar CI sem credencial como prova de sessão Live. |
| SEC-06 | Alta para operação | O certificado debug do APK instalado é diferente do certificado persistente de release. | A primeira migração exige desinstalação e perda de dados locais; bloqueada até HUMAN_GATE específico de LEANDRO. |
| SEC-07 | Média | O broker em backend/server.mjs não integra o fluxo atual e tem configuração e ameaças próprias. | Mantê-lo inativo e fora do release Android; avaliar em fase separada antes de qualquer deploy. |

## 5. Estado de publicação e governança

- PR #2 permanece OPEN / DRAFT.
- PR #3 permanece OPEN / DRAFT e depende do PR #2.
- Nenhum merge foi executado.
- Nenhuma release estável foi publicada.
- Nenhum deploy ou ativação do broker foi executado nesta preparação.
- Nenhuma API Key foi lida, exportada ou alterada nesta revisão.
- Nenhum keystore persistente foi gerado e nenhum secret de assinatura foi configurado.
- Nenhuma instalação, desinstalação, limpeza de dados ou migração foi realizada no telefone.

## 6. Limite do parecer

Este é um self-review técnico de MESTRE. Não houve auditoria independente de Emily, Ricardo ou Júlia nem gate operacional de outro agente. O parecer não substitui a revisão humana de LEANDRO e não autoriza merge, publicação, deploy ou operação no telefone.
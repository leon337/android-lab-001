# Revisão de segurança — Gemini Live Android (PRs #2 e #3)

**Escopo:** cliente Gemini Live, atualizações Android, versionamento e personalidades.
**Tipo:** revisão técnica de MESTRE baseada no código, CI e evidência manual histórica.
**Veredito:** SELF_REVIEW_PASS_WITH_RESERVATIONS.
**Auditoria independente:** não executada nesta sessão; não atribuir este parecer a Emily, Ricardo ou Júlia.

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

## 2. Ativos e controles observados

| Ativo | Controle |
|---|---|
| Chave API Gemini | AES-GCM; chave AES no Android Keystore |
| Preferências de personalidade | SharedPreferences privadas, sem armazenar a chave API |
| Token efêmero | Uso único, expiração de sessão e transporte WSS pelo header Authorization: Token |
| Áudio e transcrições | Transmissão direta ao Google; não persistidos pelo app nesta fase |
| APK de atualização | SHA-256, packageName, versionName, versionCode, minSdk e certificado verificados antes do instalador |
| Release signing key | PKCS#12 fora do Git; secrets exclusivamente no ambiente android-signing |
| Aprovação de publicação | Environment android-release com reviewer LEANDRO, branch policy main e bypass administrativo desabilitado |
| Instalação | FileProvider e confirmação nativa do Android; sem instalação silenciosa |
| Dados locais | allowBackup=false no manifesto |

O cliente não usa interceptor de logging HTTP. A sanitização de erros remove padrões conhecidos de API key e tokens efêmeros antes de apresentar mensagens.

## 3. Evidência funcional e CI

Há evidência manual histórica do APK 1.0.0 instalado no OPPO RMX3491 / Android 13: emissão de token, abertura de WebSocket, transcrição de entrada, resposta textual, mute/unmute e persistência da chave após reinício. Essa evidência antecede as alterações atuais e não comprova uma sessão Live no HEAD atual dos PRs.

As execuções GitHub Actions mais recentes concluíram com sucesso:
- PR #2: run 41749388734 — build, testes unitários, testes instrumentados e smoke de interface.
- PR #3: run 41750136570 — build, testes unitários, 17 testes instrumentados e smoke de interface.

O CI não usa uma API Key real e não inicia uma sessão Gemini Live real. Portanto, CI verde comprova build, testes e interface, não a integração ponta a ponta com a API do Google. A qualidade física do alto-falante também não foi medida remotamente.

## 4. Achados e riscos residuais

| ID | Severidade | Achado | Controle / decisão |
|---|---|---|---|
| SEC-01 | Média | A chave API existe no cliente e pode ser extraída de dispositivo comprometido, com root ou instrumentação em runtime. | AES-GCM/Keystore protege em repouso, mas não torna segredo de cliente impossível de extrair. Uso privado; não distribuir APK com chave cadastrada. |
| SEC-02 | Média | A restrição da chave Google ao package Android e ao certificado de assinatura permanente ainda depende da migração para a assinatura persistente. | Após a migração autorizada, restringir a chave ao package br.com.laboratorio.android e ao certificado de release. |
| SEC-03 | Média | Um token efêmero pode ser reutilizado se for capturado durante sua janela válida, embora seja de uso único e curta duração. | Transporte no header Authorization: Token, WSS, ausência de logging interceptor e sanitização de erros. Não registrar headers de autenticação. |
| SEC-04 | Média | O cliente conecta diretamente à API e não possui rate limit central próprio. | A cota depende do projeto Google; billing não foi habilitado nesta preparação. Reavaliar limites antes de distribuição. |
| SEC-05 | Média | Gemini Live e seus modelos usam uma superfície de API sujeita a mudanças de compatibilidade. | Validar sessão real em cada release; não tratar CI sem credencial como prova de sessão Live. |
| SEC-06 | Alta para operação | O certificado debug do APK instalado é diferente do certificado persistente de release. | A primeira migração exige desinstalação e perda de dados locais; bloqueada até HUMAN_GATE específico de LEANDRO. |
| SEC-07 | Média | O broker em backend/server.mjs não integra o fluxo atual e tem configuração/ameaças próprias. | Mantê-lo inativo e fora do release Android; avaliar em fase separada antes de qualquer deploy. |
| SEC-08 | Média | A assinatura permanente é ativo crítico de recuperação e continuidade. | Environment android-signing restrito a main; keystore e secrets ainda pendentes. Não declarar assinatura pronta até concluir o procedimento documentado. |
| SEC-09 | Média | O modelo Extended Thinking pode emitir turnComplete antes de concluir o raciocínio em segundo plano. | O cliente continua recebendo mensagens e usa interactionStatus IN_PROGRESS/IDLE para informar o estado; não encerra a sessão ao receber turnComplete. A integração real ainda precisa de validação com credencial. |

## 5. Estado de publicação e governança

- PR #2 e PR #3 continuam OPEN / DRAFT.
- PR #3 depende do PR #2.
- O workflow de release exige branch main, tag estável ancestral de main e environment protegido.
- O job de build usa android-signing; o job de publicação aguarda aprovação no environment android-release.
- O padrão é criar uma release draft; publicar imediatamente exige escolha explícita no workflow e aprovação do environment.
- Nenhuma release estável foi publicada nesta preparação.
- Nenhuma instalação, desinstalação ou limpeza foi realizada no telefone.
- Nenhuma chave API foi lida, exportada ou alterada nesta revisão.
- Keystore PKCS#12 ainda não foi gerado e os quatro secrets de assinatura ainda não foram cadastrados.

## 6. Limite do parecer

Este é um self-review técnico do MESTRE. O runtime de execução de agentes do MCF não estava conectado nesta sessão; portanto não houve execução real de Ricardo, Júlia ou Emily. Não apresentar este documento como auditoria independente ou aprovação final de merge.

A fase permanece aguardando revisão humana dos PRs, qualificação da assinatura e HUMAN_GATE separado para a migração do telefone.
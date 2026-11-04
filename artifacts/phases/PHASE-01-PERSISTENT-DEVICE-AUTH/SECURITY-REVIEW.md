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
| Release signing key | O workflow exige PKCS#12 e quatro secrets no ambiente android-signing; presença não verificada nesta sessão |
| Aprovação de publicação | O workflow declara android-release; a última documentação de setup, datada de 2026-11-03, registra reviewer LEANDRO, branch policy main e bypass administrativo desabilitado; não revalidado nesta sessão |
| Instalação | FileProvider e confirmação nativa do Android; sem instalação silenciosa |
| Dados locais | allowBackup=false no manifesto |

O cliente não usa interceptor de logging HTTP. A sanitização de erros remove padrões conhecidos de API key e tokens efêmeros antes de apresentar mensagens.

## 3. Evidência funcional e CI

Há evidência manual histórica do APK 1.0.0 instalado no OPPO RMX3491 / Android 13: emissão de token, abertura de WebSocket, transcrição de entrada, resposta textual, mute/unmute e persistência da chave após reinício. Essa evidência antecede as alterações atuais e não comprova uma sessão Live no HEAD atual dos PRs.

As últimas execuções bem-sucedidas que validaram o código do aplicativo foram:
- PR #2: run 41749388734, no commit 24cd5ac — build, testes unitários, testes instrumentados e smoke de interface.
- PR #3: run 41750136570, no commit bd79f74 — build, testes unitários, 17 testes instrumentados e smoke de interface.

Os commits documentais posteriores alteraram somente este relatório de segurança; não alteraram código, configuração de build ou testes do aplicativo. O CI não usa uma API Key real e não inicia uma sessão Gemini Live real. Portanto, CI verde comprova build, testes e interface, não a integração ponta a ponta com a API do Google. A qualidade física do alto-falante também não foi medida remotamente.

## 4. Achados e riscos residuais

| ID | Severidade | Achado | Controle / decisão |
|---|---|---|---|
| SEC-01 | Média | A chave API existe no cliente e pode ser extraída de dispositivo comprometido, com root ou instrumentação em runtime. | AES-GCM/Keystore protege em repouso, mas não torna segredo de cliente impossível de extrair. Uso privado; não distribuir APK com chave cadastrada. |
| SEC-02 | Média | A restrição da chave Google ao package Android e ao certificado de assinatura não é comprovada pelo repositório. | Confirmar a restrição no Google AI Studio/Cloud Console antes de distribuição; após a migração, vincular ao package br.com.laboratorio.android e ao certificado de release. |
| SEC-03 | Média | Um token efêmero pode ser reutilizado se for capturado durante sua janela válida, embora seja de uso único e curta duração. | Transporte no header Authorization: Token, WSS, ausência de logging interceptor e sanitização de erros. Não registrar headers de autenticação. |
| SEC-04 | Média | O cliente conecta diretamente à API e não possui rate limit central próprio. | Cotas e billing dependem da configuração do projeto Google; esses controles externos não foram inspecionados nesta revisão. Reavaliar antes de distribuição. |
| SEC-05 | Média | Gemini Live e seus modelos usam uma superfície de API sujeita a mudanças de compatibilidade. | Validar sessão real em cada release; não tratar CI sem credencial como prova de sessão Live. |
| SEC-06 | Alta para operação | A documentação de assinatura registra que a instalação atual do MVP usa certificado debug, diferente do certificado de release planejado. | Não revalidado no telefone nesta sessão. Se houver incompatibilidade de certificado, a migração exigirá desinstalação e perda de dados locais; bloqueada até HUMAN_GATE específico de LEANDRO. |
| SEC-07 | Média | O broker em backend/server.mjs não integra o fluxo atual e tem configuração/ameaças próprias. | Mantê-lo fora do fluxo do APK; avaliar em fase separada antes de qualquer deploy. |
| SEC-08 | Média | A assinatura permanente é ativo crítico de recuperação e continuidade. | O workflow exige PKCS#12 e secrets em android-signing. A documentação de setup de 2026-11-03 registrava keystore e secrets pendentes; presença atual e políticas dos environments não foram revalidadas nesta sessão. |
| SEC-09 | Média | O modelo Extended Thinking pode emitir turnComplete antes de concluir o raciocínio em segundo plano. | O cliente continua recebendo mensagens e usa interactionStatus IN_PROGRESS/IDLE para informar o estado; não encerra a sessão ao receber turnComplete. A integração real ainda precisa de validação com credencial. |

## 5. Estado de publicação e governança

- PR #2 e PR #3 continuam OPEN / DRAFT.
- PR #3 depende do PR #2.
- O workflow de release valida branch main, tag estável ancestral de main, assinatura, hashes e manifesto.
- O job de build declara o environment android-signing; o job de publicação declara android-release. Reviewer, bypass e deployment policies são descritos no documento de setup de 2026-11-03, mas não foram revalidados nesta sessão.
- O padrão do workflow é criar uma release draft; publicar exige escolha explícita no workflow e passa pelo environment android-release.
- A consulta atual ao catálogo de releases do GitHub não encontrou releases publicadas.
- Nenhuma instalação, desinstalação ou limpeza foi realizada no telefone.
- Nenhuma chave API foi lida, exportada ou alterada nesta revisão.
- MESTRE não criou nem alterou keystore ou secrets de assinatura nesta sessão; o último estado documentado em 2026-11-03 os registrava como pendentes.

## 6. Limite do parecer

Este é um self-review técnico do MESTRE. O runtime de execução de agentes do MCF não estava conectado nesta sessão; portanto não houve execução real de Ricardo, Júlia ou Emily. Não apresentar este documento como auditoria independente ou aprovação final de merge.

A fase permanece aguardando revisão humana dos PRs, qualificação da assinatura e HUMAN_GATE separado para a migração do telefone.
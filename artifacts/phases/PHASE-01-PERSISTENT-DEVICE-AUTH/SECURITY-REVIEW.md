# Revisão de segurança — Gemini Live Android

**Escopo:** PR #2 (cliente Gemini Live) e PR #3 (Fase 01: atualizações e personalidades).
**Tipo:** revisão técnica do MESTRE baseada no código, CI e evidência manual registrada.
**Veredito:** `SELF_REVIEW_PASS_WITH_RESERVATIONS`.
**Auditoria independente:** não executada nesta sessão; não atribuir este parecer a Emily, Ricardo ou Júlia.

## 1. Arquitetura efetivamente usada pelo APK

O APK usa conexão direta com a Gemini Live API. O fluxo atual não usa o broker Node/Render:

1. LEANDRO cadastra a chave API na tela de configurações.
2. `ApiKeyStore` cifra a chave com AES-GCM e guarda o ciphertext em SharedPreferences privadas.
3. A chave AES é mantida pelo Android Keystore.
4. Ao iniciar uma sessão, o app envia a chave permanente por HTTPS ao endpoint Google `v1beta/auth_tokens`, no header `x-goog-api-key`.
5. O Google retorna um token efêmero de uso único.
6. O app abre WebSocket WSS para Gemini Live usando o token efêmero como `access_token`; a chave API permanente não é enviada no WebSocket.
7. O áudio PCM 16 kHz é transmitido ao Google e o áudio PCM 24 kHz é reproduzido localmente.
8. Transcrições e estado da conversa permanecem em memória durante a sessão; não existe histórico persistente nesta fase.

O arquivo `backend/server.mjs` contém um broker experimental separado. O APK atual não o chama e esta fase não autoriza seu deploy ou ativação.

## 2. Ativos e controles observados

| Ativo | Controle |
|---|---|
| Chave API Gemini | AES-GCM; chave AES no Android Keystore |
| Preferências de personalidade | SharedPreferences privadas, sem armazenar a chave API |
| Token efêmero | Uso único, expiração de sessão e transporte WSS |
| Áudio e transcrições | Transmissão direta ao Google; não persistidos pelo app nesta fase |
| APK de atualização | SHA-256, packageName, versionName, versionCode, minSdk e certificado verificados antes do instalador |
| Release signing key | PKCS#12 fora do Git, secrets no ambiente protegido `android-release` |
| Instalação | FileProvider e confirmação nativa do Android; sem instalação silenciosa |
| Dados locais | `allowBackup=false` no manifesto |

O cliente não usa interceptor de logging HTTP. A sanitização de erros remove padrões conhecidos de API key e tokens efêmeros antes de apresentar mensagens.

## 3. Evidência funcional disponível

A validação manual registrada na fase de setup, no OPPO RMX3491 / Android 13, observou:
- emissão do token efêmero;
- abertura e configuração da sessão WebSocket;
- transcrição de fala de entrada;
- resposta textual do Gemini;
- mute bloqueando o encaminhamento de áudio;
- unmute retomando a entrada;
- reinício do app preservando a chave cifrada e permitindo nova conexão.

Os workflows de CI validam build e testes instrumentados sem uma chave real. Eles não iniciam sessão Live nem geram consumo da API. A qualidade física do alto-falante não foi medida remotamente.

## 4. Achados e risco residual

| ID | Severidade | Achado | Controle / decisão |
|---|---|---|---|
| SEC-01 | Média | A chave API existe no cliente e pode ser extraída de dispositivo comprometido, com root ou instrumentação em runtime. | AES-GCM/Keystore protege em repouso, mas não torna segredo de cliente impossível de extrair. Uso privado; não distribuir APK com chave cadastrada. |
| SEC-02 | Média | A chave Google está restrita à Gemini API, mas a restrição de aplicativo Android ainda está ausente. | Após a migração para assinatura permanente, restringir a chave ao package `br.com.laboratorio.android` e ao certificado de release. |
| SEC-03 | Média | O token efêmero aparece na query string do handshake WebSocket. | WSS, token de uso único/curta duração e ausência de logging interceptor reduzem exposição; nunca registrar URLs completas de sessão. |
| SEC-04 | Média | O cliente conecta diretamente à API e não possui rate limit central próprio. | A cota depende do projeto Google; billing permaneceu desabilitado no setup registrado. Reavaliar antes de distribuição. |
| SEC-05 | Média | Gemini Live usa superfície de API/modelos sujeita a mudanças de compatibilidade. | Manter validação manual por release e não tratar CI sem chave como prova de sessão real. |
| SEC-06 | Alta para operação | O certificado debug do APK instalado é diferente do certificado persistente de release. | Primeira migração exige desinstalação e perda de dados locais; bloqueada até HUMAN_GATE específico de LEANDRO. |
| SEC-07 | Média | O broker em `backend/server.mjs` não integra o fluxo atual e tem configuração/ameaças próprias. | Mantê-lo inativo e fora do release Android; avaliar em fase separada antes de qualquer deploy. |
| SEC-08 | Média | A assinatura permanente passa a ser ativo crítico de recuperação e continuidade. | GitHub Environment com reviewer LEANDRO, branch policy `main` e bypass administrativo desabilitado já estão configurados. Keystore e secrets ainda pendentes; não declarar assinatura pronta até concluir o procedimento local documentado. |

## 5. Estado de publicação e governança

- PR #2 continua draft e declara revisão humana antes de merge.
- PR #3 continua draft e depende do PR #2.
- O workflow de release exige branch `main`, tag estável ancestral de `main` e environment protegido.
- O padrão é criar uma release draft; publicar imediatamente exige escolha explícita no workflow e aprovação do environment.
- Nenhuma release estável foi publicada nesta preparação.
- Nenhuma instalação, desinstalação ou limpeza foi realizada no telefone.
- Nenhuma chave API foi lida, exportada ou alterada nesta revisão.

## 6. Limite do parecer

Este é um self-review técnico do MESTRE. O runtime de execução de agentes do MCF não estava conectado nesta sessão; portanto não houve execução real de Ricardo, Júlia ou Emily, nem gate operacional de LÉO. Não apresentar este documento como auditoria independente ou aprovação final de merge.

A fase permanece aguardando revisão humana dos PRs, qualificação da assinatura e HUMAN_GATE separado para a migração do telefone.

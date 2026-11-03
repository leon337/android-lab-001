# Gemini Live Android — configuração e segurança

## Objetivo
Aplicativo Android nativo de conversação por voz em tempo real com os modelos Gemini 3.8 Live e Gemini 3.8 Live Extended Thinking.

## Modelos
- `gemini-3.8-live`: conversação de baixa latência. Não enviar `thinkingConfig`.
- `gemini-3.8-live-extended-thinking`: raciocínio em segundo plano. Aceita `thinkingLevel` low, medium ou high. A interface mostra o seletor apenas para esse modelo.

## Fluxo de autenticação
1. O usuário cola a chave API na tela Configurações.
2. O app cifra a chave com AES-GCM; a chave AES é criada e mantida pelo Android Keystore.
3. Ao iniciar uma conversa, o app chama `POST https://generativelanguage.googleapis.com/v1beta/auth_tokens` usando `x-goog-api-key` no cabeçalho.
4. O pedido cria token efêmero com `uses=1`, janela de início de 60 segundos e validade de 30 minutos.
5. O app abre o WebSocket v1beta `BidiGenerateContentConstrained` usando o token efêmero no parâmetro `access_token`.
6. O modelo e as opções de áudio são enviados no primeiro pacote `setup`.

O app não envia a chave permanente no WebSocket e não incorpora nenhuma chave em BuildConfig, recursos ou código-fonte. O pedido de token não usa `liveConnectConstraints`: o broker anterior deste projeto observou rejeição HTTP 400 desse campo no ambiente Google testado. O modelo fica configurado no pacote de setup da sessão.

## Áudio
- Entrada: PCM 16 kHz, mono, 16-bit, em blocos de 40 ms.
- Saída: PCM 24 kHz, mono, 16-bit.
- Captura via AudioRecord; reprodução via AudioTrack.
- Usa fonte VOICE_COMMUNICATION e habilita cancelamento de eco/supressão de ruído quando disponíveis.
- A transcrição de entrada e saída é solicitada na configuração da sessão.
- Para Extended Thinking, o estado `interactionStatus` é acompanhado separadamente de `turnComplete`.

## Segurança e limites
- A chave API é cifrada em repouso pelo Android Keystore, mas permanece acessível ao processo do app quando necessário. Em aparelho com root ou comprometido, nenhum segredo de cliente é inviolável.
- Tokens efêmeros reduzem a exposição no WebSocket, mas não substituem autenticação segura de backend em aplicativos públicos ou multiusuário.
- Não compartilhar o APK com uma chave configurada.
- O app não realiza deploy, não ativa faturamento e não publica na Play Store.
- Os testes de CI usam chave fictícia e não iniciam sessões reais. A validação ponta a ponta requer uma chave Gemini configurada pelo usuário.

## Verificação automatizada
- Teste instrumentado da tela inicial: presença do campo API Key e dos dois modelos.
- Teste instrumentado do armazenamento: chave não aparece em texto puro no SharedPreferences, pode ser recuperada e removida.
- Teste no emulador: instala o APK, abre a tela de configuração e captura evidência visual.

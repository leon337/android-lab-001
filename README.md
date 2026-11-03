# Gemini Live para Android

Aplicativo Android nativo de conversação por voz em tempo real com os modelos **Gemini 3.8 Live** e **Gemini 3.8 Live Extended Thinking**.

## Funcionalidades

- Conversa de voz bidirecional em tempo real.
- Transcrição das falas do usuário e das respostas do Gemini na tela.
- Controles para silenciar o microfone e encerrar a sessão.
- Tela de configurações para cadastrar, alterar e remover a chave API.
- Seletor entre os dois modelos Gemini Live 3.8.
- Seleção de voz, instruções do assistente e nível de raciocínio do modelo Extended Thinking.
- Chave API cifrada com AES-GCM e protegida pelo Android Keystore.
- Token efêmero de uso único para abrir a sessão WebSocket; a chave permanente não é enviada no WebSocket.
- Interface em português brasileiro.

## Modelos disponíveis

| Nome | ID |
|---|---|
| Gemini 3.8 Live | `gemini-3.8-live` |
| Gemini 3.8 Live Extended Thinking | `gemini-3.8-live-extended-thinking` |

O nível de raciocínio (low, medium ou high) aparece somente quando o modelo Extended Thinking está selecionado.

## Como usar

1. Instale o APK no Android.
2. Abra o aplicativo; a tela de configurações será exibida no primeiro uso.
3. Crie uma chave em [Google AI Studio](https://aistudio.google.com/apikey) e cole no campo **Chave de API do Gemini**.
4. Escolha o modelo desejado e salve.
5. Toque em **Iniciar conversa** e conceda permissão ao microfone.
6. Fale naturalmente. Use **Silenciar** ou **Encerrar** durante a sessão.

## Segurança da chave

A chave API é cifrada localmente com AES-GCM; a chave AES é mantida pelo Android Keystore. O aplicativo usa HTTPS para solicitar ao Google um token efêmero e conecta o WebSocket com esse token de curta duração.

A chave permanente continua existindo no dispositivo e precisa ser usada para emitir tokens. Portanto, como qualquer segredo usado por um aplicativo cliente, ela ainda pode ser extraída em um aparelho com root, depuração comprometida ou malware. Não compartilhe o APK com sua chave já configurada e não envie a chave em mensagens.

O aplicativo não contém uma chave API fixa, não exige servidor próprio e não habilita faturamento. O consumo depende da conta, das cotas e das condições da API Gemini.

## Arquitetura

```
Configurações Compose
       ↓
LiveViewModel (StateFlow)
       ↓
ApiKeyStore (AES-GCM / Android Keystore)
       ↓
GeminiLiveClient
       ├── HTTPS: chave API → token efêmero
       ├── WebSocket v1beta: token efêmero → Gemini Live
       ├── AudioRecord: PCM 16 kHz
       └── AudioTrack: PCM 24 kHz
```

Não há proxy de áudio nem backend intermediário. O áudio é enviado diretamente ao Google pela sessão Live.

## Build e testes

O projeto usa Kotlin, Jetpack Compose, Android SDK 36, Java 17 e Gradle 8.13. O workflow **Build APK de teste** gera o APK debug. O workflow **Teste Android em emulador** executa os testes instrumentados e verifica a tela inicial sem consumir a API Gemini.

## Limitação de validação

Os testes automatizados não usam uma chave API real e não iniciam uma sessão faturável. A conexão Live real depende de você cadastrar sua própria chave no aplicativo e foi deixada para validação no seu aparelho.

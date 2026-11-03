# Gemini Live para Android

Aplicativo Android nativo de conversação por voz em tempo real com os modelos Gemini 3.8 Live e Gemini 3.8 Live Extended Thinking.

## Funcionalidades

- Conversa de voz bidirecional em tempo real.
- Transcrição das falas do usuário e das respostas do Gemini na tela.
- Controles para silenciar o microfone e encerrar a sessão.
- Tela de configurações para cadastrar, alterar e remover a chave API.
- Seletor entre os modelos Gemini Live 3.8.
- Seleção de voz e nível de raciocínio do modelo Extended Thinking.
- Personalidades: Equilibrado, Professor paciente, Programador sênior, Analista crítico, Parceiro criativo e Direto e objetivo.
- Controles comportamentais independentes: nível de detalhe, tom, iniciativa e abordagem.
- Instruções adicionais personalizadas.
- Catálogo de releases estáveis, seleção de versão e atualização por instalador Android.
- Verificação de SHA-256, pacote, versionCode e certificado de assinatura antes da instalação.

## Modelos disponíveis

| Nome | ID |
|---|---|
| Gemini 3.8 Live | `gemini-3.8-live` |
| Gemini 3.8 Live Extended Thinking | `gemini-3.8-live-extended-thinking` |

O nível de raciocínio (low, medium ou high) aparece somente quando o modelo Extended Thinking está selecionado.

## Personalidades e comportamento

A personalidade define o perfil de atuação inicial. Os controles comportamentais ajustam o perfil sem trocar a personalidade:

- **Nível de detalhe:** conciso, equilibrado ou detalhado.
- **Tom:** neutro, natural ou formal.
- **Iniciativa:** reativo ou proativo.
- **Abordagem:** prática, analítica, criativa ou questionadora.

As preferências são persistidas localmente e compõem o `systemInstruction` enviado ao Gemini na abertura de cada sessão. Elas não alteram modelo, voz ou credenciais.

## Como usar

1. Instale o APK no Android.
2. Abra o aplicativo; a tela de configurações será exibida no primeiro uso.
3. Crie uma chave em [Google AI Studio](https://aistudio.google.com/apikey) e cole no campo **Chave de API do Gemini**.
4. Escolha o modelo, a personalidade e o comportamento desejados.
5. Salve e toque em **Iniciar conversa**.
6. Use **Configurações → Versões e atualizações do aplicativo** para consultar releases disponíveis.

## Segurança da chave

A chave API é cifrada localmente com AES-GCM; a chave AES é mantida pelo Android Keystore. O aplicativo usa HTTPS para solicitar ao Google um token efêmero e conecta o WebSocket com esse token de curta duração.

A chave permanente continua existindo no dispositivo e precisa ser usada para emitir tokens. Portanto, como qualquer segredo usado por um aplicativo cliente, ela ainda pode ser extraída em um aparelho com root, depuração comprometida ou malware. Não compartilhe o APK com sua chave já configurada e não envie a chave em mensagens.

O aplicativo não contém uma chave API fixa, não exige servidor próprio para a conversa Live e não habilita faturamento. O consumo depende da conta, das cotas e das condições da API Gemini.

## Atualizações e versões

- O catálogo consulta releases públicas do repositório GitHub.
- Uma release elegível precisa conter `gemini-live-X.Y.Z.apk` e `update-manifest.json`.
- O manifesto declara pacote, versão, versionCode, minSdk, SHA-256 e certificado de assinatura.
- O app valida o APK antes de abrir o instalador nativo do Android.
- A instalação exige confirmação do usuário. Não existe atualização silenciosa.
- Versões iguais ou anteriores à instalada são bloqueadas; downgrade não faz parte da Fase 01.
- O versionCode usa `MAJOR * 10000 + MINOR * 100 + PATCH`, com MINOR e PATCH menores que 100.

### Publicar uma release

O workflow manual **Publicar versão Android assinada** recebe uma tag estável existente, como `v1.1.0`, compila o APK release, gera o manifesto e publica os dois assets.

O workflow exige estes GitHub Actions secrets:
- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

O keystore deve ser persistente, guardado fora do repositório e nunca enviado ao Git. O workflow falha fechado se algum segredo estiver ausente. Consulte [Configuração de assinatura de releases](docs/RELEASE-SIGNING-SETUP.md) para criar o keystore e cadastrar os secrets sem expor valores no chat.

**Migração inicial de assinatura:** o MVP `1.0.0` foi instalado a partir de um APK debug assinado por uma chave efêmera do runner CI. A primeira release assinada com o keystore persistente terá certificado diferente e não poderá atualizar essa instalação in-place. Será necessária uma migração única autorizada, que apaga dados locais e exige cadastrar novamente a API Key. As atualizações seguintes preservam os dados desde que o certificado permaneça o mesmo.

## Arquitetura

```
Configurações Compose
       ↓
LiveViewModel / StateFlow
       ├── ApiKeyStore (AES-GCM / Android Keystore)
       ├── PersonalityCatalog + BehaviorCatalog
       └── GeminiLiveClient
             ├── HTTPS: chave API → token efêmero
             ├── WebSocket: token efêmero → Gemini Live
             ├── AudioRecord: PCM 16 kHz
             └── AudioTrack: PCM 24 kHz

Configurações → AppUpdateViewModel
       ↓
GitHub Releases API
       ↓
Manifesto + SHA-256 + certificado
       ↓
Download privado → FileProvider → instalador Android
```

Não há proxy de áudio nem backend intermediário no fluxo Live. O áudio é enviado diretamente ao Google pela sessão Live.

## Versionamento

| Versão | Estado |
|---|---|
| `1.0.0-mvp.1` | MVP preservado, tag `v1.0.0-mvp.1` |
| `1.1.0` | Fase 01 — atualizações, seleção de versões e personalidades |

Consulte [CHANGELOG.md](CHANGELOG.md), [ADR-001](docs/architecture/ADR-001-versioning-updates-personalities.md) e [plano da Fase 01](docs/PHASE-01-VERSIONING-AND-PERSONALITIES.md).

## Build e testes

O projeto usa Kotlin, Jetpack Compose, Android SDK 36, Java 17 e Gradle 8.13. O workflow **Build APK de teste** gera o APK debug. O workflow **Teste Android em emulador** executa os testes instrumentados sem consumir a API Gemini.

A sessão Live ponta a ponta exige uma chave API real cadastrada pelo usuário e não é iniciada pelos testes automatizados.

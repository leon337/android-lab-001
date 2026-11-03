# ADR-001 — Versionamento, atualização do aplicativo e perfis comportamentais

- Status: ACEITO PARA A FASE 01
- Data: 2026-11-03
- Responsável: MESTRE
- Missão: MCF-GEMINI-LIVE-PHASE-01

## Contexto

O MVP Gemini Live já está instalado e validado em um dispositivo Android. A evolução precisa ser feita em fases independentes, com uma versão de retorno e uma distribuição de APK que não dependa de atualização manual via ADB.

A aplicação também precisa oferecer perfis de atuação configuráveis, sem acoplar a personalidade ao modelo Gemini ou à voz sintetizada.

## Decisões

### 1. Versionamento

- O MVP testado recebe a tag imutável `v1.0.0-mvp.1`, apontando para o commit `934dcafed30f7eeba4eac32d14725aa73ac37152`.
- A Fase 01 usa `versionName=1.1.0` e `versionCode=10100`.
- O versionCode segue `MAJOR * 10000 + MINOR * 100 + PATCH`; MINOR e PATCH devem ser menores que 100.
- Tags estáveis usam SemVer estrito, sem zeros à esquerda, prerelease ou build metadata.
- Cada fase mantém sua própria branch e PR.
- O APK de distribuição será publicado em GitHub Releases como asset `gemini-live-MAJOR.MINOR.PATCH.apk`, acompanhado de `update-manifest.json`.

### 2. Fonte do catálogo de versões

- O catálogo público é o endpoint de releases do repositório `leon337/android-lab-001`.
- Apenas releases publicadas, estáveis e com APK + manifesto são elegíveis.
- A consulta é manual, iniciada pelo usuário; não há polling em background.
- O app não consulta o GitHub para cada abertura nem exige backend próprio.

### 3. Integridade e autenticidade

Antes de preparar a instalação, o app valida:
1. digest SHA-256 do manifesto publicado pelo GitHub;
2. manifesto e versão do schema;
3. packageName;
4. versionName e versionCode;
5. minSdk;
6. SHA-256 do APK;
7. certificado de assinatura do APK;
8. correspondência do certificado com a instalação atual.

O parser aceita apenas assets HTTPS sob o caminho de releases do repositório oficial e exige o digest SHA-256 do manifesto.

O SHA-256 detecta corrupção. A assinatura Android é a fronteira de autenticidade que impede a substituição por APK de outro signatário.

### 4. Instalação

- O APK fica no armazenamento interno privado do aplicativo.
- FileProvider entrega um URI temporário ao instalador Android.
- O app solicita a permissão de instalação de fontes externas quando necessário.
- A instalação sempre é confirmada pelo usuário no Android.
- Não há instalação silenciosa, root, ADB embutido ou autoatualização em background.
- Versões com versionCode menor ou igual ao instalado são exibidas como não instaláveis. Rollback não faz parte da Fase 01.

### 5. Identidade de assinatura

O pipeline de release exige um keystore PKCS#12 persistente e os seguintes GitHub Environment secrets no ambiente protegido `android-release`:
- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

O keystore não pode ser versionado no Git. O ambiente `android-release` exige aprovação de LEANDRO, restringe deployments à `main` e desabilita bypass administrativo. O workflow só executa em `main`, exige tag ancestral da branch e cria release draft por padrão. O keystore e os secrets ainda estão pendentes de configuração.

**Migração inicial pendente:** o APK do MVP foi assinado por uma chave debug efêmera do runner GitHub Actions. O novo pipeline usa um certificado persistente. O Android não permite atualização in-place entre certificados diferentes. A primeira instalação do APK com assinatura persistente exigirá uma migração única, com desinstalação e perda dos dados locais/Android Keystore. O usuário deverá cadastrar novamente a chave API. Nenhuma desinstalação será executada sem autorização específica.

### 6. Personalidades e comportamento

- Personalidades são presets locais e versionados no código: Equilibrado, Professor paciente, Programador sênior, Analista crítico, Parceiro criativo e Direto e objetivo.
- Preferências comportamentais são independentes: detalhe, tom, iniciativa e abordagem.
- A instrução de sistema é composta nesta ordem: regra-base de conversa em português, instrução da personalidade, instruções comportamentais e instruções adicionais do usuário.
- A personalidade não altera modelo, voz, chave API nem configurações de segurança.
- Preferências são persistidas em SharedPreferences, sem armazenar chave API nesse arquivo.
- O usuário pode alterar a personalidade e os comportamentos a qualquer momento.

## Consequências

- O app permanece independente de um servidor de atualização próprio.
- O processo de release passa a depender de assinatura persistente e GitHub Actions secrets.
- A segurança do atualizador é fail-closed: divergência de assinatura ou integridade impede a instalação.
- O rollback de versões anteriores fica para uma fase futura.
- Memória de longo prazo, histórico, texto e ferramentas continuam fora do escopo desta fase.

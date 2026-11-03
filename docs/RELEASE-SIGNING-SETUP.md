# Configuração de assinatura de releases Android

O pipeline de release exige uma chave de assinatura persistente. A chave privada nunca deve ser commitada no Git, enviada em chat ou armazenada em artefatos públicos.

## 1. Gerar keystore PKCS#12 em máquina confiável

Com Java/Android Studio instalado:

```bash
keytool -genkeypair -v \
  -keystore gemini-live-release.p12 \
  -storetype PKCS12 \
  -alias gemini-live \
  -keyalg RSA \
  -keysize 3072 \
  -validity 10000
```

Use uma senha forte e guarde uma cópia offline do arquivo e das senhas. A senha do keystore e da chave podem ser iguais.

## 2. Codificar o arquivo para GitHub Actions

Linux/macOS:

```bash
base64 -w 0 gemini-live-release.p12
```

macOS sem `-w`:

```bash
base64 < gemini-live-release.p12 | tr -d '\\n'
```

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("gemini-live-release.p12"))
```

O texto base64 é material secreto porque permite reconstruir a chave privada.

## 3. Criar secrets no GitHub

No repositório `leon337/android-lab-001`:

Settings → Secrets and variables → Actions → New repository secret.

Criar exatamente:

| Nome | Valor |
|---|---|
| `ANDROID_SIGNING_KEYSTORE_BASE64` | Conteúdo base64 do arquivo PKCS#12 |
| `ANDROID_SIGNING_STORE_PASSWORD` | Senha do keystore |
| `ANDROID_SIGNING_KEY_ALIAS` | `gemini-live` |
| `ANDROID_SIGNING_KEY_PASSWORD` | Senha da chave |

Não cole os valores no chat. Não faça commit do arquivo .p12, certificado privado ou senhas.

## 4. Publicar uma versão

Depois que a Fase 01 estiver revisada e integrada:
1. Criar e enviar a tag estável `v1.1.0`.
2. Abrir Actions → **Publicar versão Android assinada**.
3. Informar a tag `v1.1.0`.
4. Conferir a release e os assets `gemini-live-1.1.0.apk` e `update-manifest.json`.

O workflow verifica os secrets e falha fechado quando algum estiver ausente.

## 5. Migração inicial do telefone

A instalação atual do MVP usa um certificado debug efêmero, diferente da chave persistente. O Android não permite atualização in-place entre esses certificados.

A primeira migração exigirá:
- salvar a chave API de forma segura para recadastro;
- desinstalar a versão atual;
- instalar o APK da release assinada;
- cadastrar novamente a chave API e validar a conexão.

Essa operação apaga dados locais e Android Keystore. MESTRE solicitará um HUMAN_GATE específico a LEANDRO antes de executar qualquer migração. Configurar os secrets não autoriza a desinstalação.

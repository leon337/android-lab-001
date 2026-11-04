# Assinatura permanente de releases Android

## Estado verificado em 2026-11-03

**Estado: preparação parcial.**

- GitHub Environment `android-signing`: criado, sem reviewer e com deployment branch policy restrita a `main`.
- GitHub Environment `android-release`: criado com reviewer obrigatório LEANDRO (`leon337`), branch policy somente `main` e bypass administrativo desabilitado.
- Keystore PKCS#12: ainda não gerado.
- Secrets em `android-signing`: ainda não cadastrados.
- Nenhum secret foi lido, criado ou alterado nesta etapa.

## Objetivo

Usar uma chave de assinatura persistente para todas as versões Android. O arquivo privado não pode entrar no Git, no APK, em logs ou em artefatos públicos.

## Preparação local do keystore

No notebook Ubuntu, abra um terminal e execute os passos abaixo. Não envie a senha ou o conteúdo Base64 pelo chat.

1. Crie diretórios privados:

```bash
mkdir -p "$HOME/.local/share/gemini-live-signing" "$HOME/Backups/gemini-live-signing"
chmod 700 "$HOME/.local/share/gemini-live-signing" "$HOME/Backups/gemini-live-signing"
WORK="$(mktemp -d /dev/shm/gemini-live-signing.XXXXXX)"
chmod 700 "$WORK"
```

2. Gere uma chave RSA 3072 protegida por senha. O OpenSSL solicitará a senha no terminal:

```bash
openssl genpkey -algorithm RSA -aes-256-cbc \
  -pkeyopt rsa_keygen_bits:3072 \
  -out "$WORK/private-key.pem"
```

3. Gere o certificado público:

```bash
openssl req -new -x509 \
  -key "$WORK/private-key.pem" \
  -out "$WORK/release-certificate.pem" \
  -sha256 -days 10000 \
  -subj "/CN=Gemini Live Android Release/O=Leandro Android Apps/C=BR"
```

4. Exporte o PKCS#12 com alias `gemini-live`. Informe a senha da chave e defina uma senha forte para o keystore; use a mesma senha para a chave e o keystore:

```bash
openssl pkcs12 -export \
  -out "$HOME/.local/share/gemini-live-signing/gemini-live-release.p12" \
  -inkey "$WORK/private-key.pem" \
  -in "$WORK/release-certificate.pem" \
  -name gemini-live \
  -keypbe AES-256-CBC -certpbe AES-256-CBC -macalg sha256
chmod 600 "$HOME/.local/share/gemini-live-signing/gemini-live-release.p12"
cp "$HOME/.local/share/gemini-live-signing/gemini-live-release.p12" \
  "$HOME/Backups/gemini-live-signing/gemini-live-release.p12"
chmod 600 "$HOME/Backups/gemini-live-signing/gemini-live-release.p12"
```

5. Valide o arquivo e registre o fingerprint público:

```bash
openssl pkcs12 -in "$HOME/.local/share/gemini-live-signing/gemini-live-release.p12" -noout
openssl pkcs12 -in "$HOME/.local/share/gemini-live-signing/gemini-live-release.p12" \
  -clcerts -nokeys -out "$HOME/.local/share/gemini-live-signing/release-certificate.pem"
openssl x509 -in "$HOME/.local/share/gemini-live-signing/release-certificate.pem" \
  -noout -subject -enddate -fingerprint -sha256
```

6. Guarde a senha no GNOME Passwords and Keys (GNOME Keyring), com o rótulo `Gemini Live Android release signing password`. Depois remova a chave PEM temporária e encerre o diretório de trabalho:

```bash
rm -rf "$WORK"
```

A pasta `~/Backups` está no mesmo disco físico do notebook. Faça também uma cópia offline em mídia externa segura. O arquivo PKCS#12 é protegido por senha, mas perder a senha ou a chave privada impede futuras atualizações assinadas.

## Secrets do GitHub Environment

No repositório `leon337/android-lab-001`:

Settings → Environments → `android-signing` → Environment secrets.

Cadastrar exatamente:

| Nome | Conteúdo |
|---|---|
| `ANDROID_SIGNING_KEYSTORE_BASE64` | saída Base64 do arquivo PKCS#12 |
| `ANDROID_SIGNING_STORE_PASSWORD` | senha do PKCS#12 |
| `ANDROID_SIGNING_KEY_ALIAS` | `gemini-live` |
| `ANDROID_SIGNING_KEY_PASSWORD` | senha da chave PKCS#12 |

Para obter o Base64 no notebook, use `base64 -w0 "$HOME/.local/share/gemini-live-signing/gemini-live-release.p12"` e cole diretamente no secret correspondente. Não armazene o texto Base64 em arquivos do projeto.

Não use repository secrets: o job de compilação consome os secrets do ambiente `android-signing`. O ambiente `android-release` não armazena chave privada nem senhas; ele é exclusivamente o gate de publicação.

## Proteções já configuradas no GitHub

- `android-signing`: sem reviewer; deployment branch policy restrita a `main`.
- `android-release`: reviewer obrigatório `leon337` (LEANDRO), self-review permitido, bypass de administradores desabilitado e deployment branch policy restrita a `main`.
- Branch `main`: protegida; exige PR e check `Android build + emulator` atualizado, exige resolução de conversas, bloqueia force-push e exclusão e não permite bypass administrativo. O contador de aprovações GitHub é zero porque o repositório tem uma única identidade proprietária; o gate humano MCF continua obrigatório antes do merge.

## Publicação de uma versão

Depois que os PRs do MVP e da Fase 01 forem revisados e integrados:

1. Criar uma tag SemVer estável sobre um commit integrado à `main`, por exemplo `v1.1.0`.
2. Executar Actions → **Publicar versão Android assinada**, selecionando a branch `main`.
3. Informar a tag e manter `publish=false` para preparar uma release draft, ou marcar `publish=true` se a intenção já for publicar após aprovação.
4. O job `build-release` usa `android-signing`, compila e assina o APK e disponibiliza o artifact com APK, manifesto e `release-evidence.txt`.
5. Inspecionar o artifact diretamente na execução do workflow antes de aprovar o próximo job.
6. O job `publish-release` aguarda aprovação explícita no ambiente `android-release`; após aprovação, cria a draft ou publica a release conforme a opção selecionada.

O gate de publicação ocorre **depois** da compilação e da disponibilização do artifact, não antes. O workflow valida branch, tag SemVer, versionCode, ancestralidade da tag em `main`, PKCS#12, assinatura, SHA-256, packageName, minSdk e manifesto. O catálogo do aplicativo ignora releases draft e prerelease; a versão só aparece no app depois da publicação.

## Migração inicial do telefone

A instalação atual do MVP usa um certificado debug efêmero. O certificado de release permanente será diferente; o Android não permite atualização in-place entre assinaturas distintas.

A primeira migração exigirá:
- manter a API Key atual disponível para recadastro;
- desinstalar o aplicativo atual;
- instalar o APK de release assinado;
- cadastrar novamente a API Key;
- validar uma nova sessão Gemini Live.

Essa operação apaga dados locais e Android Keystore. MESTRE não executará desinstalação, limpeza ou substituição no OPPO sem um HUMAN_GATE específico dirigido a LEANDRO. Configurar o ambiente e os secrets não autoriza a migração.

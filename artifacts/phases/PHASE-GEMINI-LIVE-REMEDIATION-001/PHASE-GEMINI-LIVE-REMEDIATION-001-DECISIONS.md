# Decisões — PHASE-GEMINI-LIVE-REMEDIATION-001

## DEC-R01 — Clientes HTTP e WebSocket separados

O timeout infinito permanece apenas no WebSocket persistente. A emissão do token usa cliente HTTP dedicado com connect/read/write/call timeout finitos. A chamada ativa é cancelada explicitamente quando a sessão é interrompida.

## DEC-R02 — Token efêmero fora da URL

O token é enviado pelo header `Authorization: Token`. A query string deixa de carregar credenciais, reduzindo exposição em logs e intermediários.

## DEC-R03 — Buffer de áudio com backpressure

O buffer é limitado a 32 chunks e 64 KiB por chunk. Ao atingir capacidade, descarta o chunk mais antigo e preserva o mais recente, priorizando baixa latência em conversação ao vivo.

## DEC-R04 — Download temporário e promoção após validação

O APK é gravado em arquivo temporário. Tamanho, SHA-256, packageName, versão, minSdk real e certificado são validados antes de promover o arquivo para o caminho final. Falhas descartam o temporário.

## DEC-R05 — Testes com servidor HTTPS local

MockWebServer usa certificado de teste confiado somente pelo cliente do teste. Nenhuma exceção de TLS ou cleartext é adicionada ao app de produção.

## DEC-R06 — Release em dois jobs e dois environments

`android-signing` entrega secrets apenas a job restrito à `main`, sem reviewer para permitir a compilação. O artifact assinado é publicado para inspeção. `android-release` é o gate com reviewer LEANDRO e só libera o job que cria draft ou publica. O ambiente de publicação não armazena a chave privada.

## DEC-R07 — CI e proteção da main

O workflow de emulador roda em `pull_request` para `main` e para a branch-base do PR #3. `main` exige PR e check estrito `Android build + emulator`, resolução de conversas, impede force-push/exclusão e bloqueia bypass administrativo. A contagem de aprovações GitHub é zero devido à identidade única do proprietário; o gate MCF humano não é substituído.

## DEC-R08 — Limites de autorização preservados

A autorização recebida cobre as correções técnicas nos PRs #2 e #3. Não cobre merge, publicação, segredo, chave API, deploy do broker ou migração do telefone. Todos permanecem bloqueados.

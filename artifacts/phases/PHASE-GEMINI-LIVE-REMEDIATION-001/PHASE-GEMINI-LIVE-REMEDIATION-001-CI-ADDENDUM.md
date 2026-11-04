# Aditivo de CI — PHASE-GEMINI-LIVE-REMEDIATION-001

## Correção adicional do workflow obrigatório

O filtro `paths` foi removido de `.github/workflows/test-emulator.yml`. Como o check `Android build + emulator` é obrigatório na proteção de `main`, ele precisa ser emitido também para PRs que alterem somente documentação. O workflow continua restrito a PRs cuja base seja `main` ou `feat/gemini-live-secure`, além de `workflow_dispatch`.

## Commits e execuções finais

- PR #2 workflow commit: `24cd5ac70af1cfb862d12c7dc8d1ded5121ec054`.
- PR #2 CI: run `41749388734` — `success`; build, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, 2 testes instrumentados e smoke de UI aprovados.
- PR #3 sincronização: merge commit `66d82caf6c9a3a8f799bf183566ff480791b549c`, incluindo o workflow atualizado sem force push.
- PR #3 CI: run `41749404671` — `success`; build, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, 17 testes instrumentados e smoke de UI aprovados.

## Estado preservado

- PR #2 e PR #3 continuam abertos e em draft.
- Nenhum PR foi integrado à `main`.
- Nenhuma release, chave, secret, deploy ou operação no telefone foi executada.
- O código Android testado permanece no commit `1cc0b79ef33b62c92a431ab3f2fb08af682b1a23`; os commits posteriores nesta cadeia alteram workflow/documentação e sincronizam a branch-base.

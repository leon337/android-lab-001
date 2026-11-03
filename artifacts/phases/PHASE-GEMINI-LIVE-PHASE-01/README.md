# PHASE-GEMINI-LIVE-PHASE-01

Missão MCF: `MCF-GEMINI-LIVE-PHASE-01`

## Resultado atual
`IMPLEMENTADO_CI_APROVADO_AGUARDANDO_GATES_DE_ASSINATURA_E_REVISÃO_HUMANA`

## Ordem de leitura
1. PLAN
2. REPORT
3. VALIDATION
4. VALIDATION-FULL
5. SMOKE
6. CHECKPOINT
7. DECISIONS
8. MISSION-TRACE
9. ARTIFACT-MANIFEST

## Entregas
- Versionamento e catálogo de releases.
- Seleção de versões estáveis.
- Download privado, SHA-256 do manifesto e APK.
- Verificação de packageName, versão, minSdk e certificado.
- Instalação confirmada pelo Android e downgrade bloqueado.
- Seis personalidades e quatro controles comportamentais.
- Persistência das preferências e systemInstruction composto.
- Workflow manual de release assinada, protegido e draft por padrão.
- Documentação, changelog, ADR e security review.

## Evidências
- Tag MVP: `v1.0.0-mvp.1`.
- Branch: `feat/phase-01-version-updates-personalities`.
- Head de código validado: `77a53f3653b04f314741416399772931765a995f`.
- PR #3 draft.
- Build: run `41730256790` — sucesso.
- Emulador: run `41731678974` — instrumentação e smoke de UI aprovados.

## GATES
- Configurar keystore persistente e quatro secrets no Environment `android-release`.
- Revisar PR #2 e PR #3 antes do merge.
- Autorizar migração única do certificado no telefone; a operação apaga dados locais e exige recadastrar a API Key.
- Nenhum desses gates será executado automaticamente.

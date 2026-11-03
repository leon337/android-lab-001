# PHASE-GEMINI-LIVE-PHASE-01

Missão MCF: `MCF-GEMINI-LIVE-PHASE-01`

## Resultado atual
`IMPLEMENTADO_AGUARDANDO_CI_E_REVIEW`

## Escopo entregue no código
- Versionamento e catálogo de releases.
- Seleção de versão.
- Download privado, SHA-256, verificação de pacote, versão e certificado.
- Instalação confirmada pelo Android.
- Seis personalidades e quatro controles comportamentais.
- Persistência das preferências.
- Workflow manual de release assinado.
- README, changelog e ADR-001.

## Branch e baseline
- Base: `934dcafed30f7eeba4eac32d14725aa73ac37152`.
- Tag: `v1.0.0-mvp.1`.
- Branch: `feat/phase-01-version-updates-personalities`.
- Versão alvo: `1.1.0` / `10100`.

## Ordem de leitura
1. PLAN
2. REPORT
3. VALIDATION
4. CHECKPOINT
5. DECISIONS
6. ARTIFACT-MANIFEST.sha256

## GATES
- Configurar os quatro secrets de assinatura no GitHub Actions.
- Autorizar a migração única do certificado no telefone; a operação apaga dados locais e exige recadastrar a API Key.
- Não executar esses gates automaticamente.

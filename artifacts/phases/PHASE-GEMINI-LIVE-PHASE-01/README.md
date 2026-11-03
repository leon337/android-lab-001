# PHASE-GEMINI-LIVE-PHASE-01

Missão MCF: `MCF-GEMINI-LIVE-PHASE-01`

## Resultado atual
`IMPLEMENTADO_CI_APROVADO_AGUARDANDO_GATES_DE_ASSINATURA`

## Entregas
- Versionamento e catálogo de releases.
- Seleção de versões estáveis.
- Download privado, SHA-256 e verificação de pacote, versão, minSdk e certificado.
- Instalação confirmada pelo Android.
- Seis personalidades e quatro controles comportamentais.
- Persistência das preferências e systemInstruction composto.
- Workflow manual de release assinada.
- Documentação, changelog e ADR-001.

## Evidências
- Tag MVP: `v1.0.0-mvp.1`.
- Branch: `feat/phase-01-version-updates-personalities`.
- Head: `eefbbb294d4be0420efb226d5bc1348956086ca7`.
- PR #3 draft.
- Build: run `41725359823`.
- Instrumentação/emulador: run `41726424995`.

## Gates
- Configurar os quatro secrets de assinatura no GitHub Actions.
- Publicar v1.1.0.
- Autorizar migração única do certificado no telefone; a operação apaga dados locais e exige recadastrar a API Key.
- Nenhum desses gates será executado automaticamente.

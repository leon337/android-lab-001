# Plano — Gemini Live Android Fase 01

## Contrato da missão
- mission_id: MCF-GEMINI-LIVE-PHASE-01
- phase_id: PHASE-GEMINI-LIVE-PHASE-01
- coordinator: MESTRE
- authority_human: LEANDRO
- risk_class: B
- permission_profile: SCOPED_WRITE
- state: IMPLEMENTATION_IN_PROGRESS

## Objetivo
Versionar o MVP atual e implementar uma fase isolada com:
1. mecanismo de consulta, seleção, download e instalação confirmada de versões Android;
2. catálogo de personalidades;
3. preferências comportamentais independentes e persistentes.

## Fonte de verdade
- Repositório MCF: `leon337/multiagent-collaboration-framework`, branch `main`.
- Repositório Android: `leon337/android-lab-001`.
- Base da fase: `934dcafed30f7eeba4eac32d14725aa73ac37152`.
- Tag de baseline: `v1.0.0-mvp.1`.
- Branch da fase: `feat/phase-01-version-updates-personalities`.
- PR anterior: #2, draft, branch `feat/gemini-live-secure`.

## Escopo
- Versionamento SemVer.
- MVP 1.0.0-mvp.1 preservado por tag Git.
- Fase 01: versionName 1.1.0, versionCode 10100.
- Catálogo de releases estáveis via GitHub Releases API.
- Manifesto JSON por release, SHA-256, packageName, versionCode, minSdk e certificado.
- Download em armazenamento interno privado.
- FileProvider e instalador Android, sempre com confirmação do usuário.
- Bloqueio de downgrade.
- Seis personalidades e quatro controles comportamentais.
- Persistência local de preferências sem armazenar a chave API nesses registros.
- Testes instrumentados para catálogo, versionamento e personalidade.
- Documentação de arquitetura, decisões e changelog.

## Fora de escopo
- Atualização silenciosa ou background polling.
- Downgrade/rollback.
- Play Store / Play App Signing.
- Histórico de conversas, memória de longo prazo e Room.
- Entrada por texto.
- Function Calling, câmera e automações.
- Migração do cliente para o broker Node/Render.

## Critérios de aceite
1. Tag MVP aponta para o commit testado.
2. A tela de configurações oferece seis personalidades.
3. Quatro preferências comportamentais podem ser ajustadas independentemente.
4. As preferências são persistidas e compõem o systemInstruction.
5. O catálogo lista somente releases estáveis com APK e manifesto.
6. APK é validado por pacote, versão, minSdk, certificado e SHA-256.
7. O instalador Android exige confirmação humana.
8. Downgrade é bloqueado.
9. Workflow de release exige keystore persistente e falha fechado sem secrets.
10. CI compila e executa os testes instrumentados.

## Riscos e gates
- O APK MVP foi assinado com chave debug efêmera do runner CI.
- O pipeline futuro usa certificado persistente; a atualização in-place entre certificados diferentes é impossível.
- A primeira migração exigirá desinstalação e apagará API Key cifrada e dados locais.
- Não executar a migração sem novo HUMAN_GATE dirigido a LEANDRO.
- Secrets de assinatura ainda não configurados; workflow de release não deve ser executado até isso ser feito.
- O catálogo GitHub usa API pública e fica sujeito ao limite de requisições; consulta é manual.

## Execução e delegação
- MESTRE: arquitetura, implementação, integração e coleta de evidências.
- Nenhuma execução independente de subagente foi invocada nesta sessão; não será alegada delegação fictícia.
- Revisão técnica e validação serão registradas com evidências do diff e CI.

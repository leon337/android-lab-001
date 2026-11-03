# Plano — Release Readiness Gemini Live Android

## Contrato da missão
- mission_id: MCF-GEMINI-LIVE-RELEASE-READINESS-001
- phase_id: PHASE-GEMINI-LIVE-RELEASE-READINESS-001
- coordinator: MESTRE
- authority_human: LEANDRO
- risk_class: B
- permission_profile: SENSITIVE_CONTROLLED
- state: IN_PROGRESS_WITH_EXTERNAL_BLOCKER

## Objetivo
Preparar o repositório, o pipeline de release e as proteções do GitHub para publicar a primeira versão estável assinada, sem realizar merge, publicação pública ou migração destrutiva do telefone nesta missão.

## Escopo
1. Consultar a versão vigente do MCF e as skills aplicáveis.
2. Verificar o estado real do notebook, GitHub, branches, PRs, tags, releases e workflows.
3. Revisar e endurecer o versionamento e o gerenciador de atualizações.
4. Criar o GitHub Environment `android-release`, com aprovação de LEANDRO e branch policy `main`.
5. Ajustar o workflow de release para executar somente em `main`, validar tags integradas e criar draft por padrão.
6. Documentar a geração da assinatura persistente e o cadastro de secrets.
7. Executar CI de build e testes instrumentados.
8. Produzir PRF completo da missão.

## Fora do escopo
- Merge de PR #2 ou PR #3.
- Publicação de release estável pública.
- Geração/upload automático do keystore e das senhas se o ambiente bloquear a operação.
- Leitura, exportação ou alteração da API Key Gemini.
- Desinstalação, limpeza de dados ou instalação no OPPO RMX3491.
- Habilitação de billing.
- Deploy do broker Node/Render.

## Critérios de aceite
1. Estado atual dos PRs, branches, tag e releases confirmado no GitHub.
2. Atualizador exige digest SHA-256 do manifesto e do APK.
3. Catálogo aceita apenas assets HTTPS do repositório esperado.
4. SemVer e versionCode têm validação estrita e sem overflow.
5. Workflow exige branch `main`, tag ancestral de `main`, assinatura e Environment protegido.
6. Publicação é draft por padrão.
7. Environment exige LEANDRO, limita deployments à `main` e desabilita bypass administrativo.
8. CI de build e testes instrumentados passa no head final.
9. Nenhum segredo aparece no Git.
10. PRs permanecem em draft até revisão humana; telefone permanece intacto.

## Riscos e gates
- O APK 1.0.0 instalado usa assinatura debug efêmera.
- A primeira migração para assinatura persistente exige desinstalação, perda dos dados locais e novo cadastro da API Key.
- Keystore e secrets são ativos críticos; nunca devem aparecer no chat ou Git.
- A geração/upload automatizada não foi concluída nesta sessão. A documentação oferece procedimento manual local.
- PR #2 contém exigência explícita de revisão humana antes de merge.
- A auditoria independente do MCF não foi executada; somente self-review do MESTRE foi realizado.

## Agentes e ferramentas
- MESTRE: coordenação, inspeção, implementação e evidências.
- O runtime local de execução multiagente não estava disponível nesta sessão; nenhum outro agente é declarado como participante.
- Ferramentas efetivamente usadas: GitHub, Git CLI e Remote Desktop Commander no notebook `leo-N43SM`.

# Plano — Remediação Gemini Live Android

## Identificação

```yaml
mission_id: MCF-GEMINI-LIVE-REMEDIATION-001
phase_id: PHASE-GEMINI-LIVE-REMEDIATION-001
risk_class: B
permission_profile: SCOPED_WRITE
coordinator: MESTRE
human_authority: LEANDRO
repository: leon337/android-lab-001
```

## Objetivo

Corrigir F-01 a F-06 da auditoria anterior e validar os dois PRs sem integrar branches, publicar releases ou alterar o dispositivo do usuário.

## Escopo autorizado

1. PR #2: timeout/cancelamento HTTP, transporte seguro do token e buffer de áudio limitado.
2. PR #3: integridade do download, validação do APK e testes automatizados.
3. CI automático para PRs e proteção de `main`.
4. Separar compilação/assinatura da aprovação/publicação.
5. Atualizar documentação e evidências MCF.

## Fora do escopo

- Merge de PRs ou mudança de base.
- Criar/publicar release pública ou draft de release.
- Gerar keystore, cadastrar/ler/alterar secrets ou API Keys.
- Deploy/ativação do broker experimental.
- Instalar, desinstalar, limpar dados ou migrar o telefone.
- Teste Gemini Live real que consuma quota/billing.

## Execução

| Ordem | Ação | Critério |
|---|---|---|
| 1 | Atualizar PR #2 | HTTP finito, cancelamento, token em header, fila limitada |
| 2 | Adicionar testes unitários e CI `pull_request` | Build, testes e smoke no runner |
| 3 | Aguardar CI do PR #2 | Check obrigatório verde |
| 4 | Atualizar PR #3 sobre o novo head do PR #2 | Histórico fast-forward, sem force push |
| 5 | Endurecer `AppUpdateRepository` | Tamanho, SHA, package, versão, minSdk e certificado |
| 6 | Adicionar testes instrumentados | Cenários válidos e inválidos |
| 7 | Reestruturar workflow de release | Artifact inspecionável antes da aprovação |
| 8 | Configurar ambientes e branch protection | Somente `main`; gates preservados |
| 9 | Executar CI final | Build, 17 testes instrumentados e smoke aprovados |
| 10 | Registrar PRF e preservar draft | Evidência rastreável; sem merge |

## Gates remanescentes

- LEANDRO revisar PR #2 e PR #3.
- Gerar PKCS#12 localmente e cadastrar quatro secrets em `android-signing`.
- Publicação só após integração autorizada, artefato inspecionado e aprovação em `android-release`.
- Migração do telefone exige HUMAN_GATE específico e separado.

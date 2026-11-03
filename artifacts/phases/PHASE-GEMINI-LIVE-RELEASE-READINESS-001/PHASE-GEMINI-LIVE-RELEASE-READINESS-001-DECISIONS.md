# Decisões — Release Readiness

| ID | Decisão | Justificativa |
|---|---|---|
| D-01 | Manter PR #2 e PR #3 em draft | PR #2 exige revisão humana expressa; PR #3 depende dele. |
| D-02 | Não fazer merge nesta missão | Não houve revisão humana nem gate de integração concluído. |
| D-03 | Criar GitHub Environment `android-release` | Secrets de assinatura devem ficar isolados do escopo geral do repositório. |
| D-04 | Exigir LEANDRO como reviewer | Publicação de APK é ação pública e precisa de aprovação humana. |
| D-05 | Restringir Environment à branch `main` e desabilitar bypass administrativo | Evita execução de release a partir de branches de feature e bypass silencioso. |
| D-06 | Criar release draft por padrão | Permite revisar APK e manifesto antes da publicação pública. |
| D-07 | Exigir digest SHA-256 do manifesto e URLs do GitHub oficial | Fecha a lacuna de confiança no manifesto e limita origem dos assets. |
| D-08 | Validar SemVer estrito e versionCode sem overflow | Evita tags ambíguas e versionCode inválido no Android. |
| D-09 | Não gerar/upload de assinatura nesta sessão | A operação automatizada não foi concluída no ambiente remoto; nenhum material secreto foi criado. O procedimento manual ficou documentado. |
| D-10 | Não alterar o telefone | A migração de certificado apaga dados locais e exige HUMAN_GATE específico. |
| D-11 | Não publicar release pública | A assinatura ainda não existe e os PRs não foram integrados. |

## Decisão de arquitetura
GitHub Releases permanece o catálogo público. O job de release só executa em `main`, valida que a tag aponta para commit integrado, assina com PKCS#12 guardado no GitHub Environment e cria draft por padrão.

## Estado da assinatura
O GitHub Environment foi criado e protegido, mas o keystore e os quatro secrets continuam pendentes. A configuração do Environment não significa que a assinatura esteja pronta.

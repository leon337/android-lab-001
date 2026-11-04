# Decisões arquiteturais — Fase 01

| ID | Decisão | Justificativa |
|---|---|---|
| ADR-001 | GitHub Releases como catálogo | Repositório já existe; evita backend de atualização próprio. |
| ADR-002 | Manifesto JSON por release | Explicita pacote, versão, minSdk, SHA-256 e certificado. |
| ADR-003 | SHA-256 + certificado Android | Hash detecta corrupção; certificado vincula atualização à instalação atual. |
| ADR-004 | FileProvider + instalador Android | Não instala silenciosamente e preserva o fluxo de consentimento do sistema. |
| ADR-005 | Consulta manual | Evita polling e consumo desnecessário da cota da API GitHub. |
| ADR-006 | Downgrade bloqueado | Android exige versionCode crescente; rollback adicionaria risco de perda de dados. |
| ADR-007 | Personalidade separada de comportamento | Permite trocar perfil e ajustar tom/detalhe/iniciativa sem acoplar ao modelo. |
| ADR-008 | Preferências em SharedPreferences | Dados pequenos e locais; não misturar com armazenamento cifrado da API Key. |
| ADR-009 | Release assinada por keystore persistente | Impede atualizações por certificados efêmeros de runners. |
| ADR-010 | Workflow manual de publicação | Nenhuma release pública é criada automaticamente por um push de tag. |
| ADR-011 | Exigir digest SHA-256 do manifesto e URL GitHub confiável | O manifesto precisa ter integridade ancorada no asset publicado; URLs externas são rejeitadas. |
| ADR-012 | Release draft por padrão e Environment com reviewer LEANDRO | A publicação pública precisa de revisão humana explícita. |
| ADR-013 | Validar SemVer estrito e proteger versionCode contra overflow | Evita versões ambíguas ou inválidas para o Android. |
| ADR-014 | Encerrar Pixel Launcher antes do smoke e limitar uiautomator dump | Mitiga instabilidade do emulador sem confundir ANR do launcher com falha do app. |

## Versões
- MVP: `v1.0.0-mvp.1` → commit `934dcafed30f7eeba4eac32d14725aa73ac37152`.
- Fase 01: `1.1.0`, versionCode `10100`.
- Formula: `major * 10000 + minor * 100 + patch`, com minor/patch menores que 100.

## Personalidades
- Equilibrado.
- Professor paciente.
- Programador sênior.
- Analista crítico.
- Parceiro criativo.
- Direto e objetivo.

## Comportamentos independentes
- Detalhe: conciso / equilibrado / detalhado.
- Tom: neutro / natural / formal.
- Iniciativa: reativo / proativo.
- Abordagem: prática / analítica / criativa / questionadora.

## Pendências de governança
- O workflow de release está implementado, mas exige quatro secrets de assinatura ainda não configurados.
- O APK atual usa certificado debug efêmero. A migração para assinatura persistente exige desinstalação e perda da API Key cifrada.
- HUMAN_GATE específico será solicitado antes de qualquer migração no telefone.

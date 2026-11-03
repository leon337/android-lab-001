# ANDROID-LAB-DB-001 — Configuração do banco Neon

**Data:** 2026-11-03
**Escopo autorizado:** criar e configurar banco PostgreSQL e registrar sua integração estrutural ao repositório Android. Sem implementar IA.
**Ferramenta visual:** MBC (Mestre Browser Control) com Brave e Neon Console; terminal remoto complementar para Git e validação.

## Recursos criados no Neon

- Projeto dedicado: `android-lab-001` (`dawn-wind-90653195`)
- Console: https://console.neon.tech/app/projects/dawn-wind-90653195/branches/br-cool-dust-bcickg1y
- Plano da organização: Free, $0/mês conforme painel Billing em 2026-11-03
- Região: AWS US East 1 (Virgínia); PostgreSQL 17
- Banco: `android_lab_001`; branch default `production` (`br-cool-dust-bcickg1y`)
- Tabela: `public.notes`; `UUID`, `content`, `suggested_summary`, `approved`, `created_at`, `updated_at`
- Constraints: nota não vazia de até 5000 caracteres; aprovação exige resumo; índice por `created_at DESC`
- Resultado verificado via Neon MCP: tabela presente, índice e constraints presentes, SELECT COUNT(*)=0.

## Privilégios

Para acesso estritamente de leitura foi criada via SQL (usando o MBC) a role `android_lab_reader` sem membership em `neon_superuser`, sem privilégios de criação de bancos ou roles, com SELECT em `public.notes` e sem INSERT/UPDATE/DELETE. Nenhuma senha está em Git ou no APK; a role não foi autenticada por cliente de rede.

Foi detectado que uma role de teste criada no Console (`android_lab_ci`) possuía `neon_superuser` por padrão. A tentativa de revogação no SQL Editor falhou por permissão insuficiente. A role temporária criada durante este trabalho foi removida do Neon; nenhuma credencial administrativa foi conectada ao CI.

## Limites e continuidade

Este checkpoint é **configuração do PostgreSQL e contrato versionado**. Não comprova persistência de anotações pelo Android: o aplicativo continua usando o Mock local e não faz requisições remotas. É deliberadamente proibido embutir uma string PostgreSQL no APK. A ligação de runtime exigirá uma API autenticada, desenho de identidade e novos testes ponta a ponta. Nenhum serviço Firebase, Gemini, Neon Auth, Functions ou AI Gateway foi ativado nesta etapa.

A Fase A de ANDROID-LAB-003 continua com revisão humana pendente e não foi reaberta ou alterada por este trabalho.

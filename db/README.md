# Android Lab 001 — Contrato PostgreSQL Neon

Projeto Neon: `android-lab-001` / `dawn-wind-90653195`.
Database: `android_lab_001`; branch principal: `production` (`br-cool-dust-bcickg1y`); PostgreSQL 17.

Os scripts em `db/neon/` descrevem o esquema já aplicado no SQL Editor do Neon por meio do MBC. O projeto Android original permanece com o Mock e não abre conexões diretas ao banco.

- `001_create_notes.sql`: tabela `public.notes`, validações e índice de ordenação.
- `002_create_readonly_role.sql`: role `android_lab_reader`, sem herança de `neon_superuser`, com direito de SELECT somente. O script deve ser executado uma única vez no banco correto. A role ainda não tem senha e não foi integrada ao Android.

Não colocar `DATABASE_URL`, senha PostgreSQL ou token dentro do APK, código-fonte ou CI logs. O aplicativo Android requer uma API HTTPS autenticada para operações reais de leitura/escrita. Tal API não foi habilitada nesta etapa; selecionar arquitetura e autenticação antes de expor acesso público.

O workflow `neon-schema-validation.yml` executa os scripts contra PostgreSQL descartável em CI, sem tocar no Neon real ou exigir segredos; isso valida que a configuração do banco é reproduzível, não a conectividade Android-Nuvem.

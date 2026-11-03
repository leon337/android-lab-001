# Changelog

Todas as alterações relevantes do Gemini Live Android serão registradas aqui.

## [1.1.0] — Fase 01 — em desenvolvimento

### Adicionado
- Catálogo de personalidades: Equilibrado, Professor paciente, Programador sênior, Analista crítico, Parceiro criativo e Direto e objetivo.
- Controles independentes de detalhe, tom, iniciativa e abordagem.
- Composição determinística da instrução de sistema a partir da personalidade, comportamento e instruções adicionais.
- Tela para consultar releases, selecionar versões e preparar instalação.
- Verificação de pacote, versionCode, certificado de assinatura e SHA-256 antes de abrir o instalador Android.
- Workflow manual para compilar, assinar e publicar releases com manifesto de atualização.

### Segurança
- Atualizações exigem confirmação no instalador nativo do Android.
- Downgrade é bloqueado nesta fase.
- A chave API e os dados locais não são enviados ao catálogo de versões.
- A release só pode atualizar uma instalação com o mesmo certificado de assinatura.

## [1.0.0-mvp.1] — 2026-11-03

### Baseline
- Conversação por voz em tempo real com Gemini Live.
- Configuração de chave API cifrada com AES-GCM e Android Keystore.
- Seleção de modelo e voz.
- Transcrição de entrada e saída.
- Controles para silenciar o microfone e encerrar sessão.
- Validado manualmente no OPPO RMX3491 / Android 13.
- Tag Git: `v1.0.0-mvp.1`.
- Commit: `934dcafed30f7eeba4eac32d14725aa73ac37152`.

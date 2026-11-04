# Fase 01 — Atualizações, seleção de versões e personalidades

## Objetivo
Evoluir o MVP 1.0.0 sem misturar funcionalidades de fases futuras.

## Entregas
- Gerenciador de versões baseado em GitHub Releases.
- Catálogo e seleção de versões estáveis publicadas.
- Download privado e verificação de SHA-256 do manifesto e do APK.
- Aceitação apenas de assets HTTPS do repositório GitHub esperado.
- Verificação do packageName, versionName, versionCode, minSdk e certificado.
- Instalação por FileProvider e instalador Android com confirmação humana.
- Seis personalidades comportamentais.
- Quatro controles independentes de comportamento.
- Persistência das preferências e composição do systemInstruction.

## Critérios de aceite
1. A tela de configurações apresenta seis personalidades.
2. Os controles de detalhe, tom, iniciativa e abordagem podem ser alterados.
3. As preferências são persistidas e compõem o systemInstruction da sessão Live.
4. O app consulta releases publicadas e lista versões que possuem APK e manifesto.
5. A versão selecionada é baixada para armazenamento privado.
6. APK com hash, pacote, versão ou certificado divergente é rejeitado.
7. APK compatível abre o instalador Android; o usuário confirma a instalação.
8. Downgrade não é oferecido.
9. O workflow de release exige assinatura persistente e não contém keystore no Git.
10. CI executa testes instrumentados de personalidade, versionamento e tela de configurações.

## Fora do escopo
- Atualização silenciosa.
- Rollback/downgrade.
- Play Store / Play App Signing.
- Histórico de conversa e Room.
- Entrada por texto.
- Memória de longo prazo.
- Function Calling, ferramentas e câmera.
- Integração do app com o broker Node/Render.

## Dependência externa
Configurar os quatro GitHub Actions secrets da assinatura persistente. A migração do APK já instalado para o certificado persistente exige uma autorização separada de LEANDRO porque a assinatura atual é diferente e a desinstalação remove a API Key cifrada e os dados locais.

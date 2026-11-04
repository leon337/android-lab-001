# Relatório — Gemini Live Android: projeto, credencial e instalação

## Estado final
`ENTREGUE`

## Execução cronológica
1. MESTRE consultou o repositório MCF oficial e leu README, mapa de estado e protocolo operacional v1.1.
2. O Desktop Commander `leo-N43SM` foi confirmado online.
3. ADB Wi-Fi identificou o OPPO RMX3491 / Android 13.
4. O APK recebido em Downloads foi identificado e comparado ao último artefato do workflow CI.
5. O projeto Google dedicado `android-lab-001-gemini-mobile` foi criado.
6. A chave `android-lab-001-gemini-mobile-android-app` foi criada e restringida à Gemini API.
7. Nenhum faturamento ou trial pago foi habilitado.
8. A tentativa de atualização por `adb install -r` encontrou incompatibilidade de assinatura.
9. A versão antiga continha dados locais e token de dispositivo cifrado. MESTRE solicitou autorização antes da operação destrutiva.
10. LEANDRO autorizou explicitamente a desinstalação e substituição.
11. A versão antiga foi removida e o APK `1.0.0` / versionCode `2` foi instalado via ADB Wi-Fi.
12. LEANDRO configurou a chave API no aplicativo e confirmou testes de conversação.
13. MESTRE verificou a existência do campo `encrypted_api_key` nas preferências privadas, sem ler ou expor seu valor.
14. A verificação de metadados mostrou 108 caracteres cifrados e ausência do prefixo textual `AIza` no arquivo de preferências.
15. O teste manual anterior confirmou solicitação de token efêmero, sessão WebSocket, transcrição de entrada e resposta textual do Gemini.
16. O teste de silêncio bloqueou o envio de áudio; reativação permitiu retomar a conversa.
17. O aplicativo foi encerrado e reaberto; a configuração permaneceu disponível e uma nova sessão foi estabelecida.

## Resultado
- Projeto Google dedicado: criado.
- Chave API dedicada: criada, restringida à Gemini API e configurada pelo usuário.
- Armazenamento: AES-GCM / Android Keystore; metadados da preferência cifrada verificados sem exposição do segredo.
- Telefone: OPPO RMX3491 / Android 13.
- APK instalado: versionName `1.0.0`, versionCode `2`.
- Billing: não habilitado.
- Restrição de aplicativo Android na chave Google: ainda não configurada.
- Áudio de saída: cliente AudioTrack é inicializado após setupComplete; a percepção física do alto-falante não foi medida remotamente.

## Limites
- Nenhuma chave ou token foi incluído neste relatório.
- Nenhuma sessão Live foi iniciada durante a preparação desta fase de versionamento.
- A versão atual usa assinatura debug do runner CI, o que exige uma migração para assinatura persistente antes de updates assinados futuros.

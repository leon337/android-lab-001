# Android Lab 001 — IA e Human-in-the-Loop

Experimento educacional: um aplicativo Android mínimo feito com Kotlin e Jetpack Compose, contendo um contador de interações.

## Arquitetura inicial
- Um módulo Android (:app), uma Activity, uma tela Compose.
- Sem backend, banco de dados, permissões sensíveis ou API paga.
- Código compilado remotamente por GitHub Actions quando autorizado.

## Construir o APK
No GitHub, execute manualmente o workflow "Build APK de teste" (workflow_dispatch).
A etapa Gradle gera app/build/outputs/apk/debug/app-debug.apk, disponibilizado como artefato do workflow.

## Termos
- **Activity**: ponto de entrada de uma tela Android.
- **Compose**: toolkit declarativo para construir a interface.
- **State**: dado que, quando muda, atualiza a interface.
- **Gradle**: ferramenta que realiza o build.
- **APK**: arquivo instalável no Android.
- **CI**: integração contínua, que automatiza verificações.

## Situação
Código e workflow preparados localmente. Ainda não foi compilado ou publicado no GitHub.

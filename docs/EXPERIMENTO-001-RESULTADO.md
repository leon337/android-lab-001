# Experimento 001 — Evidências da compilação Android na nuvem

**Data:** 2026-11-03
**Humano / autoridade final:** LEANDRO
**Orquestrador da sessão:** MESTRE
**Autorização:** criar repositório público e executar workflow gratuito de compilação APK.
**Nota de governança:** houve registro documental inspirado no MCF; não foi acionado o runtime persistente do MCF nem simulada atuação de agentes independentes.

## Entregas verificadas

- Repositório: https://github.com/leon337/android-lab-001
- Aplicativo inicial: Kotlin, Jetpack Compose, uma Activity e um contador.
- Build: https://github.com/leon337/android-lab-001/actions/runs/41639916053
- Commit exato do build: 2cd4b6988aa41ae1dda9582f45c497adb15ead2b
- Status e conclusão da execução: completed / success
- Etapas Compilar APK debug e upload-artifact: success
- Artefato GitHub: android-lab-001-apk, ID 13231159521

## Evidência local do APK

- Caminho: /home/leo/Downloads/android-lab-001-41639916053/app-debug.apk
- Tipo: Android package (APK)
- Tamanho: 11.134.051 bytes
- Integridade: unzip -tq sem erros
- Entradas inspecionadas: AndroidManifest.xml, classes.dex até classes4.dex, resources.arsc
- SHA-256: 2d747921bcf9c56579012e2aef1448038b9108f7b5f313a409384ce687b68f62

## Recuperação de falhas (CAF)

1. Run 41639599263 (commit 4718ecd): falha antes da compilação. A ação setup-android tentou instalar o pacote obsoleto tools.
2. Run 41639794774 (commit 77bce8d): alternativa sem a ação; falha, pois sdkmanager não estava no PATH.
3. Consultado o action.yml upstream: o valor padrão de packages em setup-android@v3 é tools platform-tools.
4. Run 41639916053 (commit 2cd4b69): configurado packages: platform-tools; SDK, compilação Gradle e upload passaram; APK foi inspecionado.

## Limites e próximo gate

- APK debug para experimentos, não certificado para distribuição em loja.
- Execução no celular ainda NÃO verificada.
- Artefato do workflow configurado para retenção de um dia; cópia local no notebook confirmada.
- MBC gateway ativo, mas extensão Brave apresentou EXTENSION_CONNECT_PENDING.
- Próxima decisão/validação humana: instalar, abrir o app e testar incremento do contador.
- Estado: BUILD_ENTREGUE / VALIDACAO_NO_DISPOSITIVO_PENDENTE.

# Experimento 002 — Testar Android sem celular

## Contrato e governanca
- Autoridade humana final: LEANDRO.
- Coordenacao: MESTRE.
- Autorizado: implementar e executar workflow gratuito em GitHub Actions no repositorio publico android-lab-001.
- Objetivo: validar comportamento e tela do contador usando emulador Android na nuvem.
- Fora do escopo: qualquer conta paga, publicacao em loja, uso do celular fisico e alteracao no MCF.
- Criterios de aceite: emulador inicializado, teste instrumentado aprovado, screenshots antes/depois disponiveis.
- Risco: baixo; artefatos de teste sem dados pessoais.
- Human gate: revisao de resultado e qualidade das capturas antes de nova evolucao funcional.
- Esta fase usa principios e registro MCF; nao alega runtime MCF ativo.

## Arquitetura e fluxo
GitHub Actions ubuntu-latest -> KVM -> Android SDK + Gradle -> APK + APK de teste -> Android Emulator Runner API 35 x86_64 -> Teste Compose -> PNGs e relatórios -> Artefato GitHub -> Inspecao visual por MBC.

## Conceitos
- AVD: Android Virtual Device.
- ADB: Android Debug Bridge.
- Teste de instrumentacao: teste executado em um dispositivo Android real ou virtual.
- Compose UI Test: framework que verifica comportamento de telas Compose.
- Evidencia visual: imagem capturada em ponto especifico do teste.

## Controle de custos
GitHub Actions em runners padrao de repositorios publicos; nenhum servico pago configurado.

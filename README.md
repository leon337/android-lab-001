# Android Lab — Kotlin, IA simulada e Human-in-the-Loop

Laboratório educacional Android nativo, construído com **Kotlin**, **Jetpack Compose**, **ViewModel** e testes instrumentados no emulador do GitHub Actions, sem utilizar celular físico.

## Experimentos
- **001 — APK Android na nuvem:** build e artefato disponíveis no GitHub Actions.
- **002 — Emulador Android:** teste de regressão do contador, ADB e capturas verificáveis.
- **003 A — Anotações com Mock AI:** escrever uma anotação, gerar sugestão determinística e aprovar/descartar no aplicativo.

**Atenção:** o recurso de "resumo" do Experimento 003 A é uma simulação local. Não é um modelo de IA, não faz inferência e não usa a internet. Nenhuma credencial, projeto Firebase ou serviço de IA real foi configurado.

## Arquitetura da Fase A
```
Compose (MainActivity / tela)
       ↓ estado e ações
NotesViewModel (StateFlow, viewModelScope)
       ↓ contrato
AIRepository
       ↓ implementação atual
MockAIRepository
```
O contrato `AIRepository` permite implementar um provedor real somente em fase futura, após novo gate humano.

## Testes e evidências
Execute manualmente o workflow **Teste Android em emulador** em Actions. Ele compila o aplicativo e os testes, inicia o Android virtual, roda três testes instrumentados, interage com o app via ADB e publica PNGs e XMLs de resultados.

- [Plano da Fase A](docs/EXPERIMENTO-003-FASE-A-PLANO.md)
- [Relatório da Fase A](docs/EXPERIMENTO-003-FASE-A-RESULTADO.md)
- [Resultado do Experimento 002](docs/EXPERIMENTO-002-RESULTADO.md)

## Conceitos
- **Compose**: toolkit declarativo para interfaces Android.
- **ViewModel**: preserva e gerencia o estado da tela.
- **StateFlow**: fluxo observável de estados.
- **Repository**: contrato de acesso a dados e serviços, isolando a interface.
- **Mock**: implementação artificial de comportamento previsível para testes.
- **HITL**: pessoa supervisiona e decide aprovar ou descartar a sugestão.
- **AVD**: dispositivo Android virtual do emulador.
- **CI**: execução automatizada de builds e testes.

## Limites do laboratório
Sem APIs de IA externas, faturamento, credenciais de nuvem, banco de dados ou publicação na Play Store. O registro e os relatórios seguem práticas de execução verificável e gates humanos, sem alegar ativação de um runtime multiagente.

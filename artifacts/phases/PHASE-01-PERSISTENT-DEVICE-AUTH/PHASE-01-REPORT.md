# Relatório — PHASE-01: autenticação persistente do dispositivo

## Estado

EM_REVISAO. A implementação e a validação automatizada estão concluídas no branch de feature, mas a fase não está fechada: falta auditoria independente MCF, gate de Léo e teste real da sessão WebSocket após deploy autorizado.

## Resultado técnico

O broker passou a emitir credenciais de dispositivo auto-contidas, assinadas por HMAC-SHA256, com validade de 365 dias. O Android cifra a credencial com AES-GCM usando uma chave mantida pelo Android Keystore e renova automaticamente quando restam 30 dias.

O código de ativação não é salvo no Android. A chave permanente Gemini continua exclusivamente no backend. Cada nova conversa solicita ao broker um token efêmero Gemini de uso único.

A revisão do SDK oficial identificou que a conexão WebSocket com token efêmero deve usar BidiGenerateContentConstrained em v1alpha. O cliente Android usava v1beta; a divergência foi corrigida no commit fdd8b8c. A criação REST do token continua em v1beta/auth_tokens.

## Alterações

- Backend: POST /auth/enroll e POST /auth/refresh; validação HMAC; compatibilidade legada temporária em /live/token; rate limit de emissão de sessão por dispositivo em memória.
- Android: DeviceCredentialStore com Android Keystore + AES-GCM; ativação única; renovação automática; campo de código oculto após pareamento.
- Gemini Live: modelo gemini-3.8-live-extended-thinking, thinking level LOW, entrada PCM 16 kHz e saída PCM 24 kHz; WebSocket v1alpha para token efêmero.
- Testes: 13 testes de backend; 5 testes instrumentados; build APK e smoke visual aprovados no commit atual.
- Regressão: seletores de teste alinhados à interface; espera explícita no descarte do resumo; script do emulador trata diálogos transitórios do Pixel Launcher.
- Documentação: broker README, documentação Gemini Live, plano, relatório, decisões, segurança, validação, smoke, checkpoint e mission trace.

## Commits relevantes

- 6da9450 — integração inicial do cliente Gemini Live com broker efêmero.
- 6058057 — interpretação de interactionStatus e teste do painel sem pareamento.
- 3265b71 — uso do WebSocket constrained.
- 098bd85 — diagnóstico de rejeição de token sem expor segredos.
- 80b95ce — token efêmero básico aceito pelo provedor.
- 72e47e5 — autenticação persistente do dispositivo.
- 5ac61fc — testes alinhados ao fluxo de pareamento.
- d0eaf01 — recuperação do diálogo de ANR no emulador.
- f9b66a1 — tratamento do diálogo Wait após ações do smoke.
- fdd8b8c — WebSocket v1alpha para token efêmero.

## Validação no commit fdd8b8c

- Backend: GitHub Actions run 41698184335, success, 13 testes.
- Build APK: GitHub Actions run 41698189589, success.
- Emulador: GitHub Actions run 41698195157, success, 5/5 testes instrumentados, 0 falhas, 0 erros e smoke aprovado.
- APK debug: SHA-256 49022acddbe9da829ae27896b1c5546b521da9eea0607f530d8099b5aed0c39b.
- Varredura simples de strings em classes.dex: nenhum padrão de chave Google, LAB_PAIRING_CODE ou GEMINI_API_KEY encontrado. Isso não equivale a auditoria binária completa.

## Evidência visual e MBC

MBC 0.5.0 estava conectado, bridge ready, extensão connected e sem erro corrente. Uma nova aba foi aberta no Brave para o PR #2 e a navegação foi confirmada pelo snapshot semântico. O PR foi observado como aberto e draft. Nenhum segredo foi lido ou mostrado.

## Governança e limites

- Base do PR: main em d891e601d1792adf62c43e5bcbdba8185aab98d4.
- Head: fdd8b8ccae1abd3cdfa7ced0446d6d5248e01c57.
- PR #2 permanece OPEN/DRAFT; não houve merge nem alteração em main.
- Nenhum deploy ou alteração de ambiente Render foi executado.
- Nenhuma variável, chave, serviço, banco, Firebase, Neon ou billing foi alterado nesta fase.
- O novo APK não foi instalado no telefone físico porque as rotas novas ainda não estão implantadas.
- Não houve sessão WebSocket real com o novo APK; HTTP 200 de token e testes automatizados não provam áudio Gemini funcional.
- O runtime local-agent-runtime exigido pela skill MCF-EXECUTE-LOCAL-TEAM não está disponível. Não foram simuladas contribuições de agentes independentes. Auditoria Emily e gate Léo permanecem pendentes.

## Próximas ações

1. Obter revisão independente MCF e gate interno de Léo quando o runtime estiver disponível.
2. Corrigir ou aceitar explicitamente os riscos residuais de segurança, em especial ausência de rate limit em /auth/enroll e ausência de revogação individual persistente.
3. Preparar HUMAN_GATE para deploy do backend no serviço Render existente, sem novas variáveis nem billing.
4. Após autorização, validar enrollment, refresh, token efêmero e sessão WebSocket real com o telefone/emulador.
5. Remover a compatibilidade legada somente após confirmar a migração do APK.

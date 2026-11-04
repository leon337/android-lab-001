# PHASE-01 — Autenticação persistente do dispositivo

- **Missão:** MCF-20261103-ANDROID-GEMINI-DEVICE-AUTH
- **Fase:** PHASE-01-PERSISTENT-DEVICE-AUTH
- **Classe de risco:** B — autenticação, credenciais e acesso a serviço externo
- **Estado inicial:** PLANEJADO
- **Autoridade humana:** LEANDRO
- **Coordenação:** MESTRE
- **Fonte técnica:** leon337/android-lab-001, branch feat/gemini-live-secure
- **Fonte de governança:** MCF v1.2.0 / protocolo operacional unificado

## Objetivo

Eliminar a digitação repetida do código compartilhado do laboratório, substituindo-a por pareamento único do dispositivo, credencial assinada e renovável no broker, e armazenamento criptografado no Android Keystore. Preservar a chave permanente Gemini exclusivamente no backend e manter o Mock da Fase A.

## Escopo autorizado

1. Adicionar POST /auth/enroll e POST /auth/refresh ao broker.
2. Emitir credenciais de dispositivo stateless, assinadas por HMAC-SHA256, com validade de 365 dias e renovação automática dentro dos 30 dias finais.
3. Derivar a chave de assinatura do segredo já existente LAB_PAIRING_CODE, com separação de domínio; não criar variável de ambiente nova.
4. Validar a credencial em POST /live/token, mantendo compatibilidade temporária com o código antigo para APKs anteriores.
5. Armazenar o token no Android Keystore + AES-GCM; manter somente o UUID de instalação em preferências sem segredo.
6. Mostrar o campo de ativação somente quando o dispositivo ainda não estiver pareado.
7. Adicionar testes de backend e instrumentados de armazenamento seguro.
8. Atualizar documentação e evidências da fase.

## Fora do escopo

- Alterar GEMINI_API_KEY ou LAB_PAIRING_CODE no Render.
- Deploy, alteração de configuração ou variáveis do serviço Render.
- Merge do PR #2, release pública ou publicação na Play Store.
- Firebase, Neon, banco novo, cobrança ou novos serviços pagos.
- Implementar autenticação multiusuário pública, OAuth ou revogação individual persistida.
- Embutir segredo, chave Gemini ou código de pareamento no APK, BuildConfig, logs ou repositório.

## Decisões e premissas

- O broker atual já responde /health e emite token efêmero quando recebe o código correto; a validação anterior identificou 69 caracteres no campo Android e 64 no arquivo local.
- O Render Free não oferece persistência local garantida. O token de dispositivo será auto-contido e validado criptograficamente; nenhuma lista de dispositivos será gravada no filesystem efêmero.
- A credencial é bearer e fica vinculada logicamente ao UUID de instalação, mas não oferece revogação individual sem estado persistente. Rotacionar LAB_PAIRING_CODE invalida todas as credenciais assinadas por ele.
- O token de dispositivo não é uma sessão Gemini permanente. Cada conversa continua usando token efêmero de uso único emitido pelo Google.
- A validade de 365 dias é renovada automaticamente quando restarem 30 dias; reinstalação, limpeza dos dados ou rotação do segredo exigem novo pareamento.

## Critérios de aceite

1. Sem código válido, /auth/enroll retorna 401; com código válido e UUID v4, retorna credencial assinada e expiração.
2. /auth/refresh renova credencial válida; token adulterado ou expirado recebe 401.
3. /live/token aceita credencial assinada; a compatibilidade legada não permite acesso sem segredo válido.
4. A chave Gemini nunca sai do backend.
5. Android armazena o token criptografado com chave mantida pelo Android Keystore; o valor em claro não aparece nas preferências.
6. O aplicativo solicita ativação apenas uma vez e renova a credencial sem ação do usuário.
7. Testes de backend, build APK e testes instrumentados passam.
8. O Mock/HITL existente permanece intacto.
9. Nenhuma mudança no Render, merge ou release ocorre nesta fase sem gate separado.

## Riscos e controles

| Risco | Controle |
|---|---|
| Roubo do token do dispositivo | Android Keystore + AES-GCM; token nunca vai para logs |
| Vazamento do código de ativação | Uso apenas no enrollment; não persistir nem compilar no APK |
| Token copiado para outro dispositivo | Tratar como bearer; não afirmar vinculação física forte |
| Perda de validade/rotação de segredo | Re-pareamento controlado; rotação invalida todos os tokens |
| Reinício do Render Free | Tokens stateless; não depender de filesystem ou banco local |
| Abuso de emissão de tokens Gemini | Rate limit por dispositivo em memória, como proteção complementar |
| Deploy prematuro | Deploy Render e alteração de ambiente explicitamente fora do escopo |

## Validação

- node --test backend/server.test.mjs
- :app:assembleDebug
- :app:connectedDebugAndroidTest via GitHub Actions Android Emulator API 35
- smoke de instalação/abertura no telefone via ADB Wi-Fi, sem alegar sessão Gemini real antes do deploy autorizado.

## Seleção de competências MCF

A matriz vigente recomenda, para mudança em agente/IA, a sequência:
Mestre → Miriam → Tiago → Rafael → Ricardo → Júlia → Beatriz → Augusto → Emily → Léo → Gabriel.

Para a execução desta fase também são necessárias as skills:
- MCF-RECOVER-CONTEXT — Miriam;
- MCF-IMPLEMENT-CHANGE — engenharia;
- MCF-REVIEW-CODE — Vinícius/Lucas/Ricardo;
- MCF-SECURITY-REVIEW — Ricardo/Júlia;
- MCF-RUN-TESTS — Renato;
- MCF-TRACE-MISSION — Augusto;
- MCF-CLOSE-PHASE — Carmem/Emily/Léo/Mestre;
- MCF-GIT-PR-RELEASE — Gabriel.

**Disponibilidade real:** o local-agent-runtime não está instalado no notebook (command -v sem resultado) nem consta entre as ferramentas disponíveis nesta sessão. A skill MCF-EXECUTE-LOCAL-TEAM determina fallback fail-closed quando o executor está indisponível. Portanto, não atribuímos contribuições cognitivas independentes a esses agentes. MESTRE executou diretamente as operações comprovadas por ferramentas; as revisões independentes e o gate de Léo permanecem pendentes. A fase não será declarada conforme à ESEV completa enquanto esses gates não forem executados.

## Ordem real observada

1. MESTRE recuperou as fontes de continuidade e consultou o estado atual dos dois repositórios.
2. MESTRE verificou branch, commits, PR e workflows pelo GitHub CLI/API.
3. MESTRE usou MBC 0.5.0 para abrir e inspecionar visualmente o PR #2; o navegador estava conectado e a página do PR foi confirmada.
4. MESTRE inspecionou o código e comparou o uso de WebSocket com o SDK oficial Google.
5. MESTRE corrigiu a versão do endpoint WebSocket e enviou o commit fdd8b8c.
6. MESTRE iniciou backend CI, build APK e testes instrumentados/smoke no emulador.
7. O fechamento documental e o gate interno permanecem condicionados à conclusão do workflow e à revisão independente.

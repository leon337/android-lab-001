# Plano — Gemini Live Android: projeto, credencial e instalação

## Contrato da missão
- mission_id: MCF-GEMINI-LIVE-ANDROID-SETUP-001
- phase_id: PHASE-GEMINI-LIVE-ANDROID-SETUP-001
- coordinator: MESTRE
- authority_human: LEANDRO
- risk_class: B
- permission_profile: SENSITIVE_CONTROLLED
- state: BLOCKED_ON_DEVICE_REPLACEMENT_APPROVAL

## Objetivo
Criar um projeto Google AI Studio dedicado ao app Gemini Live, gerar uma chave API dedicada e limitada à Gemini API, configurar a chave no aplicativo Android sem expô-la, instalar o APK no telefone de LEANDRO via ADB Wi-Fi e validar a inicialização.

## Escopo
- Google AI Studio / Google Cloud project e API key dedicada.
- Manter projeto no nível gratuito; não habilitar faturamento ou trial pago.
- Usar API key somente para emitir token efêmero; não incluir segredo no APK, fonte, Git ou logs.
- Instalar o APK no OPPO RMX3491 via ADB Wi-Fi.
- Salvar chave usando AES-GCM e Android Keystore do aplicativo.
- Validar estado local sem revelar chave ou token.

## Fora de escopo
- Ativar faturamento, associar cartão, migrar para plano pago.
- Publicar na Play Store ou fazer deploy.
- Compartilhar chave por chat, arquivo texto, GitHub ou código-fonte.
- Apagar dados da versão anterior sem autorização explícita de LEANDRO.
- Iniciar conversa Live faturável sem necessidade de validação autorizada.

## Fontes de verdade
- MCF: `leon337/multiagent-collaboration-framework`, branch `main`; README, `docs/MCF-CURRENT-STATE.md` e protocolo operacional v1.1.
- Projeto Android local: `/home/leo/Projetos/android-lab-001`, branch `feat/gemini-live-secure`, HEAD observado `934dcafed30f7eeba4eac32d14725aa73ac37152`.
- Google AI Studio live, projeto `android-lab-001-gemini-mobile`.
- ADB device live: OPPO RMX3491, Android 13.

## Critérios de aceite
1. Projeto Google dedicado existe e permanece no nível gratuito.
2. Chave API dedicada criada e API restriction limitada à Gemini API.
3. Segredo transferido diretamente para o campo protegido do app; sem aparecer em logs ou conversa.
4. App instalado no telefone alvo e versão confirmada.
5. Chave persistida cifrada; texto puro ausente das preferências.
6. App abre e permanece pronto sem iniciar sessão Live automaticamente.
7. PRF completo, checksums e decisão de gate registrados.

## Riscos e controles
- API key é segredo de cliente: AES-GCM/Android Keystore, restrição de API e uso somente para token efêmero.
- Restrição Android por package+SHA-1 não aplicada nesta build; o cliente atual ainda não envia `X-Android-Package`/`X-Android-Cert`. Não alegar restrição de aplicativo.
- Projeto sem faturamento; não acionar botão de billing/trial.
- APK atual tem assinatura diferente da versão já instalada; `adb install -r` falhou com `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- Dados da instalação antiga contêm token de dispositivo cifrado e device ID; desinstalar apaga esses dados e a chave Android Keystore correspondente. Exige HUMAN_GATE de LEANDRO.

## Autorização
LEANDRO autorizou criação do projeto, criação de chave e configuração/instalação do app. A autorização não inclui apagar os dados da instalação anterior. Nenhuma ação de faturamento foi autorizada.

## Equipe / execução
- MESTRE: coordenação, interação Google AI Studio, ADB e evidências.
- Delegação nativa a subagentes: não invocada; nenhuma execução independente será alegada.
- Revisão de segurança aplicada pelo MESTRE conforme contrato MCF de Ricardo; gate destrutivo permanece reservado a LEANDRO.

## Plano de validação
- Conferir projeto e restrições no Google Cloud Console.
- Conferir identidade do telefone e ADB.
- Instalar APK; confirmar package/version.
- Configurar chave por clipboard sincronizado via scrcpy, sem imprimir conteúdo.
- Confirmar presença do registro cifrado sem ler valor.
- Limpar clipboard e confirmar app pronto.

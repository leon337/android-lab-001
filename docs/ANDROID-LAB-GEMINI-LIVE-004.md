# Android Lab — Integração Gemini Live Extended Thinking (fase separada)

**Modelo:** `gemini-3.8-live-extended-thinking` com thinking level LOW.
**Conta Gemini:** projeto `android-lab-001-gemini` ID `gen-lang-client-0423414497`.
**Estratégia:** microfone Android em PCM 16 kHz -> Live WebSocket usando token temporário -> áudio do modelo PCM 24 kHz -> alto-falante Android.
**Backend:** Node.js `backend/server.mjs`, chave Gemini apenas em variável de ambiente do servidor, autenticação por código de laboratório informado ao abrir o app e nunca persistido em BuildConfig.

## Restrições de segurança e custo

- Não embutir `GEMINI_API_KEY` no APK ou no GitHub; token permanente não vai ao telefone.
- Proteger o token broker por segredo de pareamento, usado apenas como MVP privado de um usuário; não é autenticação escalável de produção.
- Free Tier Gemini sujeito a cotas e privacidade: conteúdo da conta de nível gratuito pode ser usado pelo provedor para melhorar produtos; utilizar falas fictícias.
- Não habilitar billing, Firebase, Neon AI Gateway ou recursos pagos.
- A Fase A segue pendente de aprovação humana; o fluxo Mock original mantém seus testes.

## Critérios de aceitação

1. API /health pública responde 200 sem expor segredo.
2. API /live/token retorna 401 sem credencial; só emite token com código de acesso.
3. Android solicita permissão de microfone, obtém token, abre WebSocket v1beta para `gemini-3.8-live-extended-thinking`, envia áudio PCM e reproduz áudio recebido.
4. Build APK e testes instrumentados aprovados no emulador; teste real exige configurar credenciais protegidas no backend e gerar sessão Live efetiva.
5. Pull request draft, sem merge automático na main, sujeito a HUMAN_GATE.

Sem o passo 4 de sessão Live real, a integração não deve ser relatada como ponta a ponta concluída.

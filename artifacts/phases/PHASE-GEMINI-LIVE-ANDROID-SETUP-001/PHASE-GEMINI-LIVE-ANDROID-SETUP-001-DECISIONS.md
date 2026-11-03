# Decisões

| Ordem | Decisão | Evidência | Responsável |
|---|---|---|---|
| 1 | Usar projeto Google dedicado para o app móvel | Projeto anterior tinha chave com nome de servidor | MESTRE |
| 2 | Criar chave nova; não reutilizar chave de servidor | Projeto anterior android-lab-001-gemini já possuía chave | MESTRE |
| 3 | Manter projeto no nível gratuito e não habilitar billing | AI Studio/Cloud Console exibiu nível gratuito | MESTRE |
| 4 | Restringir chave à Gemini API | Tela de edição da chave mostrou API selecionada | MESTRE |
| 5 | Não aplicar restrição Android ainda | Cliente atual não envia headers de identidade Android | MESTRE |
| 6 | Não desinstalar pacote antigo sem autorização | ADB retornou INSTALL_FAILED_UPDATE_INCOMPATIBLE | MESTRE |
| 7 | Desinstalar e substituir versão antiga após HUMAN_GATE | LEANDRO autorizou explicitamente em 2026-11-03 | LEANDRO |
| 8 | Confirmar armazenamento cifrado sem ler o segredo | Entry encrypted_api_key presente; sem prefixo AIza | MESTRE |
| 9 | Manter app no nível gratuito | Nenhum billing/trial ativado | MESTRE |

## Gate concluído
LEANDRO autorizou a remoção da versão antiga e a perda dos dados locais/Android Keystore. A nova versão foi instalada e a chave foi configurada pelo usuário.

## Pendências remanescentes
- Restrição de aplicativo Android na chave Google.
- Migração futura para assinatura de release persistente; exige novo gate porque a assinatura atual do APK debug é diferente.

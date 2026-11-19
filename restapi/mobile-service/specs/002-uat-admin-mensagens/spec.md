# Especificação de Funcionalidade: Habilitação de Perfil Administrador em Usuário UAT para Mensagens e Avisos (`mobile-service`)

**Branch da Feature**: `feat/uat-admin-mensagens`  
**Criado em**: 2026-11-19  
**Status**: Aprovado para Implementação  
**Subprojeto**: `restapi/mobile-service`  
**Feature Base**: `001-mensagens-avisos`

## Cenários de Usuário e Testes *(obrigatório)*

### História de Usuário 1 - Usuário UAT Membro (`uat.membro`) como Administrador de Avisos no Emulador Android (Priority: P1)

Como desenvolvedor/testador utilizando o emulador Android com contas sintéticas UAT (`uat.synthetic-users.enabled=true`), quero que o usuário **`uat.membro`** (`Lucas Almeida Monteiro`, matrícula `900001`) possua a permissão `androidmppb.mensagens.admin` e que **`uat.servidor`** (`Mariana Costa Ribeiro`, matrícula `900002`) permaneça como destinatário comum, para testar o ciclo completo de envio (`Broadcast`, `Multicast`, `Unicast`), recebimento, entrega, leitura e ciência no simulador sem alterar contas reais de produção.

**Teste Independente**: Autenticar com `uat.membro`, verificar `GET /api/mensagens/permissao-admin` (`podeEnviar = true`), enviar um aviso para `uat.servidor` (`900002`), autenticar com `uat.servidor` (`podeEnviar = false`), listar e abrir a mensagem confirmando entrega/leitura/ciência.

**Critérios de Aceitação**:

1. **Dado** `uat.synthetic-users.enabled=true` fora do profile `review`, **Quando** `uat.membro` autentica, **Então** `SyntheticTokenAuthenticationProvider` inclui a authority `androidmppb.mensagens.admin` em suas funcionalidades permitidas.
2. **Dado** `uat.servidor` ou qualquer conta `REVIEW_GOOGLE_*` / `REVIEW_APPLE_*`, **Quando** autentica, **Então** **não** recebe a authority `androidmppb.mensagens.admin` (preservando o comportamento de destinatário comum e o isolamento de Store Review).
3. **Dado** `uat.membro` autenticado no painel de envio, **Quando** consulta prévia de alcance, busca destinatários ou envia aviso (`Broadcast`, `Multicast` ou `Unicast`), **Então** o sistema resolve sua identidade sintética (`900001`) sem exigir registro em `autorizacao.usuario` e inclui as identidades UAT (`900001` e `900002`) como destinatários válidos.
4. **Dado** `uat.servidor` (`900002`) autenticado na caixa de entrada, **Quando** lista, recebe, lê ou confirma ciência de uma mensagem enviada por `uat.membro`, **Então** o sistema resolve sua matrícula sintética (`900002`) e atualiza os contadores de acompanhamento.

## Requisitos *(obrigatório)*

- **FR-001**: `SyntheticTokenAuthenticationProvider` **DEVE** conceder a funcionalidade `androidmppb.mensagens.admin` exclusivamente à identidade `IdentidadeSintetica.UAT_MEMBRO` (nunca às contas `UAT_SERVIDOR` nem `REVIEW_*`).
- **FR-002**: Os serviços de mensagem (`MensagemEnvioService`, `MensagemDestinatarioService`, `GrupoMensagemService`) **DEVEM** resolver matrícula e nome via `IdentidadeSintetica.porLogin(login)` quando o usuário autenticado for uma identidade sintética UAT (fallback para `PerfilUsuarioRepository.buscarPorLogin(login)` para usuários institucionais).
- **FR-003**: Quando o remetente for `IdentidadeSintetica` UAT, a resolução e busca de destinatários **DEVE** garantir a presença das identidades UAT (`900001` Membro e `900002` Servidor) para permitir testes ponta a ponta entre `uat.membro` e `uat.servidor` no simulador.
- **FR-004**: Nenhum teste existente de `Q14UsuariosSinteticosUatReviewTest` ou `STORE-REVIEW-R02` pode sofrer regressão.

# Especificação de Funcionalidade: Habilitação de Perfil Administrador em Usuário UAT para Mensagens e Avisos (`mobile-service`)

**Branch da Feature**: `feat/uat-admin-mensagens`  
**Criado em**: 2026-11-19  
**Status**: Aprovado para Implementação  
**Subprojeto**: `restapi/mobile-service`  
**Feature Base**: `001-mensagens-avisos`

## Cenários de Usuário e Testes *(obrigatório)*

### História de Usuário 1 - Usuário Sintético Membro (`uat.membro` / `membro.google`) como Administrador de Avisos no Emulador Android (Priority: P1)

Como desenvolvedor/testador utilizando o emulador Android com contas sintéticas (`uat.synthetic-users.enabled=true`), quero que os usuários sintéticos de perfil **`MEMBRO`** (**`uat.membro`**, **`membro.google`** e **`membro.apple`** — `Lucas Almeida Monteiro`, matrícula `900001`) possuam a permissão `androidmppb.mensagens.admin` e que os usuários de perfil **`SERVIDOR`** (**`uat.servidor`**, **`servidor.google`**, **`servidor.apple`** — `Mariana Costa Ribeiro`, matrícula `900002`) permaneçam como destinatários comuns, para testar o ciclo completo de envio (`Broadcast`, `Multicast`, `Unicast`), recebimento, entrega, leitura e ciência no simulador sem alterar contas reais de produção.

**Teste Independente**: Autenticar com `uat.membro` ou `membro.google`, verificar `GET /api/mensagens/permissao-admin` (`podeEnviar = true`), enviar um aviso para `900002` (`uat.servidor` / `servidor.google`), autenticar com `uat.servidor` ou `servidor.google` (`podeEnviar = false`), listar e abrir a mensagem confirmando entrega/leitura/ciência.

**Critérios de Aceitação**:

1. **Dado** `uat.synthetic-users.enabled=true`, **Quando** uma conta sintética com `PerfilFuncional.MEMBRO` (`uat.membro`, `membro.google`, `membro.apple`) autentica, **Então** `SyntheticTokenAuthenticationProvider` inclui a authority `androidmppb.mensagens.admin` em suas funcionalidades permitidas.
2. **Dado** uma conta sintética com `PerfilFuncional.SERVIDOR` (`uat.servidor`, `servidor.google`, `servidor.apple`), **Quando** autentica, **Então** **não** recebe a authority `androidmppb.mensagens.admin` (preservando o comportamento de destinatário comum).
3. **Dado** `uat.membro` ou `membro.google` autenticado no painel de envio, **Quando** consulta prévia de alcance, busca destinatários ou envia aviso (`Broadcast`, `Multicast` ou `Unicast`), **Então** o sistema resolve sua identidade sintética (`900001`) sem exigir registro em `autorizacao.usuario` e inclui as identidades sintéticas (`900001` e `900002`) como destinatários válidos.
4. **Dado** `uat.servidor` ou `servidor.google` (`900002`) autenticado na caixa de entrada, **Quando** lista, recebe, lê ou confirma ciência de uma mensagem enviada por `900001`, **Então** o sistema resolve sua matrícula sintética (`900002`) e atualiza os contadores de acompanhamento.

## Requisitos *(obrigatório)*

- **FR-001**: `SyntheticTokenAuthenticationProvider` **DEVE** conceder a funcionalidade `androidmppb.mensagens.admin` às identidades sintéticas com `PerfilFuncional.MEMBRO` (`uat.membro`, `membro.google`, `membro.apple`) e nunca às identidades com `PerfilFuncional.SERVIDOR`.
- **FR-002**: Os serviços de mensagem (`MensagemEnvioService`, `MensagemDestinatarioService`, `GrupoMensagemService`) **DEVEM** resolver matrícula e nome via `IdentidadeSintetica.porLogin(login)` quando o usuário autenticado for uma identidade sintética (fallback para `PerfilUsuarioRepository.buscarPorLogin(login)` para usuários institucionais).
- **FR-003**: Quando o remetente for `IdentidadeSintetica`, a resolução e busca de destinatários **DEVE** garantir a presença das identidades sintéticas (`900001` Membro e `900002` Servidor) para permitir testes ponta a ponta entre `membro.google`/`uat.membro` e `servidor.google`/`uat.servidor` no simulador.
- **FR-004**: O script DDL `2026-11-18_mensagens_avisos.sql` **DEVE** incluir os comandos `GRANT USAGE ON SCHEMA diariooficial TO android`, `GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE ... TO android` e `GRANT USAGE, SELECT ON SEQUENCE ... TO android` para evitar erro `SQLState: 42501 (permission denied for table mensagemaviso)`.
- **FR-005**: Nenhum teste existente de `Q14UsuariosSinteticosUatReviewTest` ou `STORE-REVIEW-R02` pode sofrer regressão.

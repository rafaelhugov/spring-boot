# Plano de Implementação: Usuário UAT Administrador de Mensagens (`mobile-service`)

**Branch**: `feat/uat-admin-mensagens` | **Data**: 2026-11-19 | **Spec**: [spec.md](./spec.md)

## Resumo

Habilitar a conta sintética **`uat.membro`** (`900001`) como administrador de mensagens/avisos (`androidmppb.mensagens.admin`) e permitir resolução de perfil/destinatários sintéticos UAT (`uat.membro` e `uat.servidor`) nos serviços de mensagens para testes ponta a ponta no emulador Android, mantendo `uat.servidor` (`900002`) e contas `REVIEW_*` sem permissão administrativa.

## Arquivos Afetados

1. `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/synthetic/auth/SyntheticTokenAuthenticationProvider.java`
   - Adicionar `FuncionalidadeAutorizadaResponseDto(7L, "Gerenciar Mensagens e Avisos", "androidmppb.mensagens.admin")` quando `identidade == IdentidadeSintetica.UAT_MEMBRO`.
2. `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemEnvioService.java`
   - Resolver remetente via `IdentidadeSintetica.porLogin(login)` antes de consultar `PerfilUsuarioRepository`.
   - Quando o remetente for UAT sintético, mesclar as identidades UAT (`UAT_MEMBRO` `900001` e `UAT_SERVIDOR` `900002`) na resolução de destinatários conforme a modalidade/critérios.
3. `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/MensagemDestinatarioService.java`
   - Resolver matrícula do destinatário via `IdentidadeSintetica.porLogin(login)` antes de consultar `PerfilUsuarioRepository`.
4. `restapi/mobile-service/src/main/java/mppb/restapi/mobile/feature/mensagem/service/GrupoMensagemService.java`
   - Resolver criador de grupo e incluir `uat.membro` / `uat.servidor` na busca de destinatários quando compatível com o termo pesquisado.
5. `restapi/mobile-service/src/test/java/mppb/restapi/mobile/feature/mensagem/MensagemUatAdminTest.java`
   - Testes automatizados validando que `uat.membro` recebe `androidmppb.mensagens.admin`, `uat.servidor` e `REVIEW_*` não recebem, e o fluxo envio (`uat.membro`) → leitura (`uat.servidor`) funciona sem registro em `autorizacao.usuario`.

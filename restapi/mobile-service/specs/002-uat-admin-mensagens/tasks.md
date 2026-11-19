---
description: "Tarefas para habilitar uat.membro como administrador de mensagens/avisos no simulador Android"
---

# Tarefas: Usuário UAT Administrador de Mensagens (`002-uat-admin-mensagens`)

- [x] T001 Conceder a authority `androidmppb.mensagens.admin` exclusivamente para `IdentidadeSintetica.UAT_MEMBRO` em `SyntheticTokenAuthenticationProvider.java`
- [x] T002 Adicionar resolução de identidade sintética UAT (`uat.membro` e `uat.servidor`) em `MensagemEnvioService.java`, `MensagemDestinatarioService.java` e `GrupoMensagemService.java`
- [x] T003 Criar testes unitários em `MensagemUatAdminTest.java` e executar `mvn test -pl mobile-service` validando zero regressões na suíte completa (incluindo `Q14UsuariosSinteticosUatReviewTest`)

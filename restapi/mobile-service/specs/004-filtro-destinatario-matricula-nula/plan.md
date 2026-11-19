# Plan 004 — Filtro de Destinatários sem Matrícula

1. Adicionar helper `filtrarDestinatariosValidos` em `MensagemEnvioService.java` e aplicá-lo em `resolverDestinatarios` e `publicarParaDestinatarios`.
2. Atualizar `processarAgendadas` para usar `resolverDestinatariosComUat`.
3. Validar com `mvn -pl mobile-service test`.

# Spec 004 — Filtro de Destinatários sem Matrícula no Envio de Mensagens

## Contexto
Durante `BROADCAST` ou processamento de mensagens agendadas (`MensagemAgendadaScheduler`), a consulta de destinatários ativos retorna registros funcionais com `matricula` nula (ex.: colaboradores/estagiários sem matrícula), causando `ConstraintViolationException` (`PSQLException: null value in column "destinatariomatricula" of relation "mensagemdestinatario" violates not-null constraint`).

## Requisitos Funcionais
- **RF-01**: `MensagemEnvioService.resolverDestinatarios` deve filtrar e deduplicar destinatários, descartando qualquer registro com `matricula` nula ou em branco.
- **RF-02**: `MensagemEnvioService.publicarParaDestinatarios` deve ignorar defensivamente qualquer destinatário sem `matricula` válida antes de persistir `MensagemDestinatario`.
- **RF-03**: `MensagemEnvioService.processarAgendadas` deve usar `resolverDestinatariosComUat` preservando destinatários UAT quando o remetente for `900001`.

# Plan 003 — Foto do Remetente nos Avisos

1. Adicionar `getBytesFotoPorMatricula(String matricula)` em `CarteiraService.java`.
2. Adicionar `remetenteMatricula` em `MensagemRecebidaResponse.java`.
3. Adicionar `GET /api/mensagens/remetentes/{matricula}/foto` em `MensagemDestinatarioController.java`.
4. Adicionar/executar testes unitários no `mobile-service`.

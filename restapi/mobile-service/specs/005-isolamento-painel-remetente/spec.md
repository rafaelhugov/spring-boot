# Spec 005 — Isolamento do Painel de Avisos por Usuário Remetente

## Contexto
Atualmente `GET /api/mensagens/admin/enviadas`, `GET /api/mensagens/admin/{id}/acompanhamento`, `PATCH /api/mensagens/admin/{id}/cancelar` e `PATCH /api/mensagens/admin/{id}/arquivar` não filtram/validam `remetenteMatricula`, fazendo com que todos os administradores vejam e alterem avisos uns dos outros.

## Requisitos Funcionais
- **RF-01**: `GET /api/mensagens/admin/enviadas` deve retornar exclusivamente os avisos cuja `remetenteMatricula` pertence ao usuário autenticado.
- **RF-02**: `detalhar`, `cancelarAgendada` e `arquivarRemetente` devem validar que o aviso pertence à `remetenteMatricula` do usuário autenticado (lançando `IllegalStateException` / `AccessDeniedException` quando não pertencer).

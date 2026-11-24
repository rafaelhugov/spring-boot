# Feature Spec: 008 - Correções CRUD de Grupos e Busca sem Acento

## Problemas
1. `PUT /api/mensagens/admin/grupos/personalizados/{id}` falhava/não persistia ao atualizar nome e membros devido à ordem de flush entre `deleteByGrupoId` e `saveAll` na mesma transação.
2. Busca de destinatários (`/api/mensagens/admin/destinatarios/buscar`) não ignorava acentuação (`like` direto sem normalização).

## Requisitos
1. Garantir exclusão imediata dos vínculos antigos (`@Modifying(clearAutomatically = true, flushAutomatically = true)`) e persistência completa de nome, descrição e membros na edição de grupos.
2. Normalizar acentos no termo buscado e na coluna `p.nome` via `translate(...)` no PostgreSQL e `Normalizer` em Java.

# Plan: 008 - Correções CRUD de Grupos e Busca sem Acento

- `GrupoMensagemMembroRepository.java`: usar `@Modifying(clearAutomatically = true, flushAutomatically = true)` + `@Query("delete ...")` em `deleteByGrupoId`.
- `GrupoMensagemService.java`: `saveAndFlush(grupo)` e normalização de acentos na busca UAT.
- `RhDestinatarioRepository.java`: `translate(lower(coalesce(p.nome, '')), ...)` + normalização `Normalizer.Form.NFD` no termo.
- `GrupoMensagemTest.java`: testar atualização de nome + membros e busca sem acento.

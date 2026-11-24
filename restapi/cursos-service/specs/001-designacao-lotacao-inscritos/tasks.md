# Tasks 001: Designação e Lotação de Participantes Internos (`cursos-service`)

- [x] T001 Atualizar `master` e criar branch dedicada `feat/designacao-lotacao-inscritos`
- [x] T002 Criar `spec.md`, `plan.md`, `tasks.md` e atualizar `.specify/feature.json`
- [x] T003 Adicionar `getLotacaoNome()` e `getDesignacaoNome()` em `ServidorProjection.java`
- [x] T004 Atualizar `ServidorRepository.java` (`buscarServidorPorId` e projeções) para consultar lotação (`public.setor` / `public.orgao`) e designação ativa (`diariooficial.mapa_por_membro_vw`)
- [x] T005 Adicionar campos `lotacao` e `designacao` em `ParticipanteDto.java`
- [x] T006 Atualizar `ParticipanteService.toEnrichedDto` para preencher `lotacao` e `designacao` para participantes `INTERNO`
- [x] T007 Atualizar `InscricaoExcelExporter.java` para incluir colunas `Lotação` e `Designação`
- [x] T008 Criar/atualizar testes unitários para enriquecimento de `ParticipanteDto` e exportação Excel
- [x] T009 Executar testes do `cursos-service` e validar compilação

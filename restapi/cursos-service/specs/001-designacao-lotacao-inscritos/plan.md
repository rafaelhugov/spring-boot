# Plan 001: Designação e Lotação de Participantes Internos (`cursos-service`)

## 1. Arquitetura e Abordagem
A solução aproveita o fluxo centralizado de enriquecimento em `ParticipanteService.toEnrichedDto(Participante)`, que já é utilizado por:
- `InscricaoService.toDto(Inscricao)` (usado na listagem paginada de inscritos em `CursoController` e na exportação Excel).
- `ParticipanteService` e demais pontos que expõem `ParticipanteDto`.

## 2. Arquivos Impactados
1. `ServidorProjection.java` (`feature/common/projection/`):
   - Adicionar `String getLotacaoNome();`
   - Adicionar `String getDesignacaoNome();`
2. `ServidorRepository.java` (`feature/common/repository/`):
   - Atualizar `buscarServidorPorId` (e demais queries de projeção `ServidorProjection`, quando aplicável, garantindo consistência) com `LEFT JOIN public.setor`, `public.orgao`, `public.pessoa` para `lotacaoNome` e subquery em `diariooficial.mapa_por_membro_vw` para `designacaoNome`.
3. `ParticipanteDto.java` (`feature/participante/dto/`):
   - Adicionar `private String lotacao;`
   - Adicionar `private String designacao;`
4. `ParticipanteService.java` (`feature/participante/service/`):
   - Em `toEnrichedDto(Participante)`, preencher `dto.setLotacao(servidor.getLotacaoNome())` e `dto.setDesignacao(servidor.getDesignacaoNome())` apenas para `TipoParticipanteEnum.INTERNO`.
5. `InscricaoExcelExporter.java` (`feature/inscricao/util/`):
   - Incluir colunas `"Lotação"` e `"Designação"` no cabeçalho e nas linhas exportadas (vazio para externos/nulos).
6. Testes unitários (`src/test/java/...`):
   - Adicionar testes cobrindo `ParticipanteService.toEnrichedDto` (interno com lotação/designação vs externo) e `InscricaoExcelExporter`.

## 3. Verificação
- Executar `mvn test -pl cursos-service` (ou `mvn -Dtest=... test`) no diretório `spring-boot/restapi`.

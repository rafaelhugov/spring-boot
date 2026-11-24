# Spec 001: Exibir Designação e Lotação para Participantes Internos (MPPB)

## 1. Contexto e Objetivo
No Sistema de Cursos (`cursos-service`), os participantes inscritos do tipo `INTERNO` (servidores e membros do MPPB) atualmente exibem apenas nome, e-mail e cargo. O objetivo desta especificação é disponibilizar, diretamente a partir do banco de dados institucional (ATENDIMENTO), as informações de **Designação** e **Lotação** para participantes internos, mantendo-as ocultas/nulas para participantes `EXTERNO`, tanto na API de listagem/detalhes quanto na exportação em Excel.

## 2. Requisitos Funcionais
- **RF-01 — Enriquecimento dinâmico de Participante Interno**: Ao converter um `Participante` do tipo `INTERNO` (com `idServidor != null`) para `ParticipanteDto` via `ParticipanteService.toEnrichedDto`, o sistema deve consultar no banco de dados e preencher os campos `lotacao` e `designacao`.
- **RF-02 — Atualização automática para inscritos existentes**: Como os dados são consultados dinamicamente do banco de dados (`public.servidor`, `public.setor`, `public.orgao`, `public.pessoa` e `diariooficial.mapa_por_membro_vw`), tanto novas inscrições quanto participantes internos já cadastrados anteriormente devem receber automaticamente os valores atualizados.
- **RF-03 — Participantes Externos**: Para participantes do tipo `EXTERNO`, os campos `lotacao` e `designacao` devem permanecer `null` (sem preenchimento).
- **RF-04 — Exportação Excel (`InscricaoExcelExporter`)**: O arquivo `.xlsx` exportado pelo endpoint de participantes deve incluir as colunas `Lotação` e `Designação`, preenchendo com o valor correspondente quando `INTERNO` (ou vazio `""` quando nulo ou `EXTERNO`).

## 3. Regras de Negócio e Fonte de Dados
- **Lotação**: Obtida via `diariooficial.mapa_por_membro_vw` (`ativo = 'A'`), utilizando `designacao_tipoletra = 'T'` (Titularidade) quando `Membro` e `designacao_tipoletra = 'L'` (Lotação) quando `Servidor` (com fallback para `public.setor` / `public.orgao`).
- **Designação**: Obtida via `diariooficial.mapa_por_membro_vw m` com `m.ativo = 'A'` e `m.servidor_matricula::text = s.matricula`, retornando **todas** as designações ativas distintas concatenadas por `; ` utilizando apenas os campos `m.funcao_nome` e `m.orgao_nome` (ex.: `funcao_nome - orgao_nome` quando ambos existem e são distintos, ou apenas o campo preenchido).

## 4. Critérios de Aceitação
- `GET /api/cursos/{cursoId}/participantes` retorna `participante.lotacao` e `participante.designacao` preenchidos para inscritos `INTERNO` (quando existentes no banco) e `null` para inscritos `EXTERNO`.
- `GET /api/cursos/{cursoId}/participantes/export` gera planilha Excel com as colunas `Lotação` e `Designação` (em branco para externos).
- Todos os testes unitários e de compilação do `cursos-service` passam após as alterações.


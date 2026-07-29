# Fase 2A - Cobertura da API de Cursos

Data: 2026-07-29.

Branch: `test/phase-2a-cursos-api`.

Commit base: `cfbd52b06c6aa7bad7cbbc75c61b07ebb2f6296f`.

Escopo: API administrativa de Cursos em `spring-boot/restapi/cursos-service`. O frontend `mppb-vue/cursos-web` nao foi alterado. Nao inclui Fase 2B, Solicitacoes, Dashboard, Participantes, Ministrantes, APIs publicas ou mudancas de contrato do frontend.

## Resultado

Fase 2A pronta para MR apos a correcao final de erro de integridade no `DELETE /api/cursos/{id}`. O reactor completo passou com 141 testes, 0 falhas, 0 erros e 0 ignorados.

## Baseline 117 versus 119

A diferenca foi de contagem, nao de remocao de teste:

| Verificacao | Resultado |
|---|---|
| `git rev-parse HEAD` | `cfbd52b06c6aa7bad7cbbc75c61b07ebb2f6296f` |
| `git rev-parse origin/master` | `cfbd52b06c6aa7bad7cbbc75c61b07ebb2f6296f` |
| `git diff --name-status origin/master...HEAD` | vazio |
| Surefire baseline | 117 testes reais |
| Contagem textual anterior | 119 ocorrencias iniciadas por `@Test` |

A causa objetiva: a contagem textual de 119 inclui `@Testcontainers` e `@TestInstance` em `ArquivoSecurityIntegrationTest`. O Surefire executa 117 testes reais no baseline.

## Inventario dos 24 testes

| Metodo de teste | Endpoint | Cenario | Status esperado | Regra comprovada |
|---|---|---|---|---|
| `listarCursosSemAutenticacaoRetorna401` | `GET /api/cursos` | listagem sem autenticacao | `401` | endpoints administrativos exigem autenticacao |
| `listarCursosComTokenInvalidoRetorna401` | `GET /api/cursos` | token rejeitado pelo `AuthServiceClient` mockado | `401` | token invalido nao acessa negocio |
| `usuarioComApenasCursosAcessoNaoListaCursosAdministrativos` | `GET /api/cursos` | usuario com apenas `cursos.acesso` | `403` | manter cursos exige `cursos.manter.cursos` |
| `listarCursosComPaginacaoFiltroOrdenacaoEStatusValido` | `GET /api/cursos` | pagina, tamanho, titulo, status e sort validos | `200` | filtros e ordenacao funcionam com status valido |
| `filtroTituloComSqlInjectionNaoRetornaTodosOsCursos` | `GET /api/cursos` | titulo com payload de SQL injection | `200` | filtro permanece parametrizado e nao amplia resultado |
| `listarCursosComPaginaOuTamanhoInvalidosRetorna400` | `GET /api/cursos` | `page < 0` e `size < 1` | `400` | paginacao invalida tem resposta controlada |
| `consultarCursoExistenteRetornaDetalhesDosTresStatus` | `GET /api/cursos/{id}` | cursos `EM_CONSTRUCAO`, `PUBLICADO`, `FINALIZADO` | `200` | detalhe traduz os tres estados reais |
| `consultarCursoInexistenteRetorna404SemDetalhesInternos` | `GET /api/cursos/{id}` | id inexistente | `404` | erro controlado sem SQL/exception |
| `criarCursoValidoRetorna201ComStatusInicialEHashGerado` | `POST /api/cursos` | payload minimo valido | `201` | criacao define `EM_CONSTRUCAO` e gera hash |
| `criarCursoComTituloAusenteRetorna400` | `POST /api/cursos` | titulo obrigatorio ausente/vazio | `400` | Bean Validation de campo obrigatorio |
| `criarCursoComCargaHorariaOuPercentualInvalidosRetorna400` | `POST /api/cursos` | carga horaria e percentual invalidos | `400` | validacao de formato/faixa |
| `criarCursoSemPublicoAlvoRetorna400` | `POST /api/cursos` | publico alvo vazio | `400` | curso exige publico alvo |
| `criarCursoComCampoDesconhecidoRetorna400` | `POST /api/cursos` | campo desconhecido no JSON | `400` | `FAIL_ON_UNKNOWN_PROPERTIES` continua ativo |
| `criarCursoComIdStatusHashOuDatasInternasNoPayloadRetorna400` | `POST /api/cursos` | tentativa de mass assignment com `id`, `status`, hash e datas | `400` | grupo `Create` rejeita `id` enviado |
| `atualizarCursoValidoPreservaStatusHashEDataCriacao` | `PUT /api/cursos/{id}` | update comum com tentativa de alterar campos imutaveis | `200` | update preserva status, hash e data de criacao |
| `atualizarCursoInexistenteRetorna404` | `PUT /api/cursos/{id}` | id inexistente | `404` | update inexistente nao cria curso |
| `atualizarCursoComIdDivergenteRetorna422ENaoAlteraBanco` | `PUT /api/cursos/{id}` | id do payload diverge da URL | `422` | rejeita divergencia e nao altera registro |
| `removerCursoValidoRetorna204` | `DELETE /api/cursos/{id}` | curso sem dependencia | `204` | exclusao administrativa funciona |
| `removerCursoInexistenteRetorna404` | `DELETE /api/cursos/{id}` | id inexistente | `404` | delete inexistente e controlado |
| `removerCursoComDependenciasFalhaSemRemocaoParcial` | `DELETE /api/cursos/{id}` | curso com aula, participante e inscricao | `409` | dependencia bloqueia exclusao sem remocao parcial e sem detalhe interno |
| `listarArquivosComprobatoriosRetornaVinculosDoCurso` | `GET /api/cursos/{cursoId}/arquivos-comprobatorios` | comprovante ja vinculado | `200` | listagem retorna vinculo e metadados |
| `uploadComprobatorioValidoEmCursoFinalizadoCriaArquivoEVinculo` | `POST /api/cursos/{cursoId}/arquivos-comprobatorios` | upload PDF em curso finalizado | `201` | cria `ged.arquivo` e vinculo |
| `uploadComprobatorioRejeitaCursoInexistenteNaoFinalizadoVazioOuTipoInvalido` | `POST /api/cursos/{cursoId}/arquivos-comprobatorios` | curso inexistente, nao finalizado, vazio e mime invalido | `404`/`422` | rejeicoes nao persistem arquivo/vinculo |
| `removerArquivoComprobatorioRemoveVinculoEArquivo` | `DELETE /api/cursos/{cursoId}/arquivos-comprobatorios/{comprovanteId}` | remocao do comprovante | `204` | remove vinculo e arquivo GED |

Classificacao: autorizacao 1-3; consulta/listagem/status 4-8; criacao/validacao/mass assignment 9-14; edicao 15-17; exclusao/rollback 18-20; arquivos associados 21-24.

## Status e transicoes

Estados reais no enum `StatusCursoEnum`: `EM_CONSTRUCAO`, `PUBLICADO`, `FINALIZADO`. `INSCRICOES_ENCERRADAS` nao existe no enum atual.

Cobertura confirmada:

| Regra | Situacao |
|---|---|
| status inicial na criacao | coberto por `criarCursoValidoRetorna201ComStatusInicialEHashGerado` |
| tentativa de fornecer status no POST | coberta dentro do payload de mass assignment, rejeitado por `id` no grupo `Create` |
| preservacao do status em PUT comum | coberta por `atualizarCursoValidoPreservaStatusHashEDataCriacao` |
| consulta dos tres estados reais | coberta por `consultarCursoExistenteRetornaDetalhesDosTresStatus` |
| publicacao valida | nao ha endpoint explicito; ocorre pelo recalculo de aulas |
| publicacao de curso incompleto | nao ha endpoint explicito; regra fica no fluxo de aulas |
| encerramento de inscricoes | nao ha estado `INSCRICOES_ENCERRADAS` no enum |
| finalizacao | feita por `CursoConclusaoJob`/`CursoRepository.atualizarCursosConcluidos`, sem endpoint administrativo |
| transicao invalida/regressao | nao ha endpoint de transicao dedicado |

## Recalculo de aulas

O ajuste em `CursoService.update` impede que um PUT comum chame `AulaService.recalcularDadosCurso` e sobrescreva status, data inicial, data final e carga horaria derivados. O teste `atualizarCursoValidoPreservaStatusHashEDataCriacao` cobre a preservacao de status, hash e criacao; a analise estatica confirmou que o recalculo continua no fluxo correto de aulas:

| Fluxo | Chamada confirmada |
|---|---|
| criar aula | `AulaService.createAula` chama `recalcularDadosCurso(curso)` |
| editar aula | `AulaService.updateAula` chama `recalcularDadosCurso(curso)` |
| remover aula | `AulaService.deleteAula` chama `recalcularDadosCurso(curso)` |

Teste funcional amplo de recalculo por alteracao de aula pertence a uma fase de Aulas/Frequencia, pois exige expandir fixtures e asserts fora do nucleo administrativo de Curso.

## Paginacao, filtros e ordenacao

Coberto: pagina e tamanho validos, `page < 0`, `size < 1`, sort valido por titulo, filtro por titulo, tentativa de SQL injection, status valido. Status invalido segue o contrato real da especificacao/JPA e nao recebeu bloqueio artificial. Size excessivo, propriedade de sort inexistente, direcao de sort invalida, filtro vazio, filtro muito longo e caracteres especiais permanecem riscos de contrato Spring/Data para fase posterior.

## Exclusao e dependencias

Dependencias testadas diretamente: `curso.aula`, `curso.inscricao`, `curso.participante` e `curso.cursopublicoalvo`. Comprovantes sao cobertos em endpoint especifico de arquivos associados. `cursoministrante` e limpo pelo service antes da exclusao, mas nao foi duplicado em teste nesta fase. Imagem de cabecalho, template, certificados e outros relacionamentos ficam como pendencia especifica por relacionamento real.

O caso com dependencias agora retorna `409` sanitizado e preserva curso/inscricao no banco, sem SQL, constraint, CPF ou token no corpo.

## Correcoes realizadas

- `POST /api/cursos` usa validacao `Default + Create`.
- `POST /api/cursos` rejeita `id` fornecido pelo cliente.
- `PUT /api/cursos/{id}` rejeita divergencia entre id da URL e id do payload com `422`.
- `GET /api/cursos` rejeita `page < 0` e `size < 1`.
- JSON invalido/desconhecido retorna `400` sanitizado via `ApiInputExceptionHandler`.
- Violacao de integridade retorna `409` sanitizado via `ApiInputExceptionHandler`.
- Criacao define `EM_CONSTRUCAO` quando o status vem nulo.
- Criacao gera `hashInscricao` quando o hash vem nulo.
- Atualizacao direta do curso nao dispara recalculo indevido de aulas.

## Cobertura

| Metrica | Antes | Depois | Variacao |
|---|---:|---:|---:|
| Linhas | 22,52% | 26,27% | +3,75 p.p. |
| Branches | 12,23% | 14,44% | +2,21 p.p. |
| Metodos | 27,14% | 32,67% | +5,53 p.p. |
| Instrucoes | 18,33% | 22,02% | +3,69 p.p. |

Cobertura por classe:

| Classe | Linhas | Branches | Metodos |
|---|---:|---:|---:|
| `ApiInputExceptionHandler` | 11/11, 100,00% | n/a | 3/3, 100,00% |
| `CursoController` | 20/87, 22,99% | 10/12, 83,33% | 11/34, 32,35% |
| `CursoService` | 75/201, 37,31% | 14/90, 15,56% | 15/37, 40,54% |
| `CursoRepository` | nao listado pelo JaCoCo CSV como classe instrumentada | n/a | n/a |
| `CursoDto` | nao listado pelo JaCoCo CSV como classe instrumentada | n/a | n/a |

## Execucao final

| Comando | Exit code | Resultado |
|---|---:|---|
| `mvn -q clean -Dtest=CursoApiIntegrationTest test` | 0 | 24 testes, 0 falhas, 0 erros, 0 ignorados |
| `mvn -pl restapi-common,cursos-service -am clean verify` | 0 | reactor com 141 testes em `cursos-service` e 2 em `restapi-common`, build success |
| `git diff --check` | 0 | sem whitespace errors |
| `git diff --name-status origin/master...HEAD` | 0 | vazio, branch criada no mesmo commit do `origin/master` |

O aviso do Surefire apareceu no shutdown apos os testes: `Surefire is going to kill self fork JVM...`. O exit code foi 0, os XMLs ficaram completos e nao houve impacto funcional comprovado. `docker ps` mostrou apenas containers persistentes de desenvolvimento (`cursos-web`, `cursos-api`, `cursos-db`), sem containers Testcontainers remanescentes.

## Riscos pendentes

- Contratos especificos de sort invalido, size excessivo e filtros extremos ainda podem ser formalizados em fase posterior.
- Publicacao/finalizacao/regressao de status nao possuem endpoint dedicado; devem ser tratados na fase de Aulas/Frequencia ou job de conclusao.
- Imagem de cabecalho, template de certificado e certificados ficam como verificacao direcionada fora deste nucleo.
- Aviso de shutdown lento do Surefire e compreendido e nao bloqueante, mas segue pendente sem impacto funcional comprovado.

## Sugestao de commits

1. Testes de integracao da API administrativa de Cursos.
2. Correcoes minimas de contrato e seguranca da API de Cursos.
3. Documentacao final da Fase 2A.

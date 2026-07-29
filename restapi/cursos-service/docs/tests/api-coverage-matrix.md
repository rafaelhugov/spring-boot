# Matriz de Cobertura API - Seguranca Backend

Data: 2026-07-24.

Escopo: autenticacao, autorizacao, IDOR, upload, download, documentos e manuais administrativos relacionados a arquivos do `cursos-service`.

| Endpoint | Metodo | Perfil | Risco | Cenario | Classe de teste | Metodo de teste | Automatizado | Resultado |
|---|---|---|---|---|---|---|---|---|
| `/api/arquivos/{id}` | GET | nenhum | autenticacao ausente | download privado sem auth | `ArquivoSecurityIntegrationTest` | `endpointPrivadoSemAutenticacaoRetorna401` | Sim | `401` |
| `/api/arquivos/{id}` | GET | token invalido | autenticacao invalida | Auth API rejeita token | `ArquivoSecurityIntegrationTest` | `endpointPrivadoComTokenInvalidoRetorna401` | Sim | `401` |
| `/api/arquivos/{id}` | GET | sem authority | autorizacao | usuario autenticado sem authority | `ArquivoSecurityIntegrationTest` | `endpointPrivadoComUsuarioSemAuthorityRetorna403` | Sim | `403` |
| `/api/arquivos/{id}` | GET | authority inexistente | autorizacao | `cursos.arquivo.baixar` nao concede acesso | `ArquivoSecurityIntegrationTest` | `authorityInexistenteNaoConcedeAcesso` | Sim | `403` |
| `/api/arquivos/100` | GET | `cursos.acesso` | propriedade correta | Usuario A baixa programacao da propria solicitacao | `ArquivoSecurityIntegrationTest` | `usuarioADonoDaSolicitacaoDeveBaixarArquivo100` | Sim | `200` |
| `/api/arquivos/100` | GET | `cursos.acesso` | IDOR A/B | Usuario B tenta arquivo 100 da solicitacao de A | `ArquivoSecurityIntegrationTest` | `usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | curriculo de ministrante | dono da solicitacao acessa curriculo | `ArquivoSecurityIntegrationTest` | `solicitanteAcessaCurriculoDeMinistranteDaPropriaSolicitacao` | Sim | `200` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | IDOR curriculo | outro usuario acessa curriculo de A | `ArquivoSecurityIntegrationTest` | `outroUsuarioNaoBaixaCurriculoDeMinistranteDaSolicitacaoDoUsuarioA` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | IDOR foto | outro usuario acessa foto de A | `ArquivoSecurityIntegrationTest` | `outroUsuarioNaoBaixaFotoDeMinistranteDaSolicitacaoDoUsuarioA` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.manter.cursos` | acesso CEAF | admin baixa foto/curriculo de solicitacao | `ArquivoSecurityIntegrationTest` | `administradorBaixaFotoDeMinistranteDaSolicitacao`, `administradorBaixaCurriculoDeMinistranteDaSolicitacao` | Sim | `200` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | comprovante administrativo | usuario comum baixa comprovante | `ArquivoSecurityIntegrationTest` | `usuarioComCursosAcessoNaoBaixaArquivoComprobatorioAdministrativo` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` + `cursos.analisar.solicitacoes` | bypass indevido | analisar solicitacoes sem CEAF baixa comprovante | `ArquivoSecurityIntegrationTest` | `usuarioComCursosAcessoEAnalisarSolicitacoesNaoBaixaComprovatorioAdministrativo` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.manter.cursos` | comprovante administrativo | CEAF baixa comprovante com headers | `ArquivoSecurityIntegrationTest` | `administradorBaixaArquivoComprobatorioComHeadersSeguros` | Sim | `200` |
| `/api/arquivos/{id}` | GET | `cursos.manter.cursos` | template | CEAF baixa template | `ArquivoSecurityIntegrationTest` | `administradorBaixaTemplateCertificado` | Sim | `200` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | template | participante/solicitante tenta template | `ArquivoSecurityIntegrationTest` | `participanteAutenticadoNaoBaixaTemplateCertificado`, `usuarioComCursosAcessoNaoBaixaTemplateCertificado` | Sim | `403` |
| `/api/arquivos/{id}` | GET | nenhum | template | sem autenticacao tenta template | `ArquivoSecurityIntegrationTest` | `semAutenticacaoNaoBaixaTemplateCertificado` | Sim | `401` |
| `/api/arquivos/{id}` | GET | `cursos.manter.cursos` | vinculo ambiguo | arquivo e cabecalho e template | `ArquivoSecurityIntegrationTest` | `arquivoComVinculoAmbiguoRetorna403MesmoParaAdministrador` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | arquivo orfao | arquivo sem vinculo conhecido | `ArquivoSecurityIntegrationTest` | `arquivoSemVinculoRetorna403` | Sim | `403` |
| `/api/arquivos/{id}` | GET | `cursos.acesso` | inexistente | ID nao existe | `ArquivoSecurityIntegrationTest` | `arquivoInexistenteRetorna404` | Sim | `404` |
| `/api/public/arquivos/{id}` | GET | publico | imagem publicada | cabecalho de curso publicado | `ArquivoSecurityIntegrationTest` | `imagemCabecalhoDeCursoPublicadoRetorna200NoEndpointPublico` | Sim | `200` |
| `/api/public/arquivos/{id}` | GET | publico | imagem nao publicada | cabecalho de curso em construcao | `ArquivoSecurityIntegrationTest` | `imagemDeCursoNaoPublicadoNaoRetornaNoEndpointPublico` | Sim | `403` |
| `/api/public/arquivos/{id}` | GET | publico | arquivo privado | programacao/foto/curriculo/template no publico | `ArquivoSecurityIntegrationTest` | `arquivoPrivadoNaoRetornaNoEndpointPublico`, `fotoDeMinistranteNaoRetornaNoEndpointPublico`, `curriculoDeMinistranteNaoRetornaNoEndpointPublico`, `templateDeCursoPublicadoNaoRetornaNoEndpointPublico` | Sim | `403` |
| `/api/public/arquivos/{id}` | GET | publico | inexistente | ID nao existe | `ArquivoSecurityIntegrationTest` | `arquivoPublicoInexistenteRetorna404` | Sim | `404` |
| `/api/arquivos/upload` | POST | nenhum | upload sem auth | multipart anonimo | `ArquivoSecurityIntegrationTest` | `uploadSemAutenticacaoNaoEnviaArquivo` | Sim | `401` |
| `/api/arquivos/upload` | POST | sem authority | upload sem permissao | usuario autenticado sem authority | `ArquivoSecurityIntegrationTest` | `uploadSemAuthorityNaoEnviaArquivo` | Sim | `403` |
| `/api/arquivos/upload` | POST | `cursos.acesso` | upload valido | PDF valido cria arquivo orfao | `ArquivoSecurityIntegrationTest` | `usuarioAutorizadoEnviaArquivoValido` | Sim | `200` |
| `/api/arquivos/upload` | POST | `cursos.acesso` | validacao de conteudo | executavel disfarcado como PDF | `ArquivoSecurityIntegrationTest` | `arquivoInvalidoERejeitadoAntesDaPersistencia` | Sim | `422`, 0 registros |
| `/api/cursos/{cursoId}/arquivos-comprobatorios` | POST | `cursos.manter.cursos` | associacao admin | upload cria vinculo do curso | `ArquivoSecurityIntegrationTest` | `uploadComprobatorioValidoCriaVinculoCorretoComCurso` | Sim | `201` |
| `/api/cursos/{cursoId}/arquivos-comprobatorios` | POST | `cursos.acesso` | associacao indevida | usuario comum associa comprovante | `ArquivoSecurityIntegrationTest` | `usuarioSemCeafNaoAssociaArquivoComprobatorioAoCurso` | Sim | `403` |
| `/api/cursos/{id}/documentos/gerar` | POST | nenhum/token invalido | autenticacao | sem auth ou token invalido | `ArquivoSecurityIntegrationTest` | `gerarDocumentoSemAutenticacaoRetorna401`, `gerarDocumentoComTokenInvalidoRetorna401` | Sim | `401` |
| `/api/cursos/{id}/documentos/gerar` | POST | `cursos.acesso`/inexistente | autorizacao | usuario comum ou authority errada | `ArquivoSecurityIntegrationTest` | `usuarioComCursosAcessoNaoGeraDocumentoAdministrativo`, `authorityInexistenteNaoGeraDocumentoAdministrativo` | Sim | `403` |
| `/api/cursos/{id}/documentos/gerar` | POST | `cursos.manter.cursos` | sucesso/inexistente | CEAF gera; curso inexistente | `ArquivoSecurityIntegrationTest` | `administradorGeraDocumentoAdministrativo`, `gerarDocumentoParaCursoInexistenteRetorna404` | Sim | `200`/`404` |
| `/api/manuais/pdf/administrativo` | GET | query token | token em URL | `?token=` nao autentica | `ArquivoSecurityIntegrationTest` | `manualAdministrativoComTokenNaQueryRetorna401` | Sim | `401` |
| `/api/manuais/pdf/administrativo` | GET | `Authorization: Token` + CEAF | contrato correto | header autentica e baixa manual | `ArquivoSecurityIntegrationTest` | `manualAdministrativoComHeaderTokenAutorizadoRetorna200` | Sim | `200` |

## Observacoes

- Autorizacao por authority ocorre no controller com `@PreAuthorize`.
- Autorizacao por propriedade ocorre no service com usuario autenticado e consultas JPQL reais.
- O endpoint generico permanece por compatibilidade, mas nega arquivo orfao e ambiguo.

## Fase 2A - Cursos API

Data: 2026-07-29.

Branch: `test/phase-2a-cursos-api`.

Commit base: `cfbd52b06c6aa7bad7cbbc75c61b07ebb2f6296f`.

Classe principal: `CursoApiIntegrationTest`.

| Endpoint | Metodo | Perfil | Risco | Cenario | Metodo de teste | Automatizado | Resultado |
|---|---|---|---|---|---|---|---|
| `/api/cursos` | GET | nenhum | autenticacao ausente | listagem administrativa sem auth | `listarCursosSemAutenticacaoRetorna401` | Sim | `401` |
| `/api/cursos` | GET | token invalido | autenticacao invalida | Auth API rejeita token | `listarCursosComTokenInvalidoRetorna401` | Sim | `401` |
| `/api/cursos` | GET | `cursos.acesso` | autorizacao | usuario comum tenta listar admin | `usuarioComApenasCursosAcessoNaoListaCursosAdministrativos` | Sim | `403` |
| `/api/cursos` | GET | `cursos.manter.cursos` | consulta/listagem | paginacao, filtro, sort e status valido | `listarCursosComPaginacaoFiltroOrdenacaoEStatusValido` | Sim | `200` |
| `/api/cursos` | GET | `cursos.manter.cursos` | SQL injection | payload em `titulo` nao amplia resultado | `filtroTituloComSqlInjectionNaoRetornaTodosOsCursos` | Sim | `200` |
| `/api/cursos` | GET | `cursos.manter.cursos` | paginacao invalida | `page < 0` e `size < 1` | `listarCursosComPaginaOuTamanhoInvalidosRetorna400` | Sim | `400` |
| `/api/cursos/{id}` | GET | `cursos.manter.cursos` | status/detalhe | detalhe nos estados reais | `consultarCursoExistenteRetornaDetalhesDosTresStatus` | Sim | `200` |
| `/api/cursos/{id}` | GET | `cursos.manter.cursos` | inexistente | id inexistente sem detalhe interno | `consultarCursoInexistenteRetorna404SemDetalhesInternos` | Sim | `404` |
| `/api/cursos` | POST | `cursos.manter.cursos` | criacao | payload valido | `criarCursoValidoRetorna201ComStatusInicialEHashGerado` | Sim | `201` |
| `/api/cursos` | POST | `cursos.manter.cursos` | validacao | titulo ausente | `criarCursoComTituloAusenteRetorna400` | Sim | `400` |
| `/api/cursos` | POST | `cursos.manter.cursos` | validacao | carga horaria/percentual invalidos | `criarCursoComCargaHorariaOuPercentualInvalidosRetorna400` | Sim | `400` |
| `/api/cursos` | POST | `cursos.manter.cursos` | validacao | publico alvo vazio | `criarCursoSemPublicoAlvoRetorna400` | Sim | `400` |
| `/api/cursos` | POST | `cursos.manter.cursos` | contrato JSON | campo desconhecido | `criarCursoComCampoDesconhecidoRetorna400` | Sim | `400` |
| `/api/cursos` | POST | `cursos.manter.cursos` | mass assignment | `id`, status, hash e datas internas | `criarCursoComIdStatusHashOuDatasInternasNoPayloadRetorna400` | Sim | `400` |
| `/api/cursos/{id}` | PUT | `cursos.manter.cursos` | edicao | update valido preserva imutaveis | `atualizarCursoValidoPreservaStatusHashEDataCriacao` | Sim | `200` |
| `/api/cursos/{id}` | PUT | `cursos.manter.cursos` | inexistente | update de curso ausente | `atualizarCursoInexistenteRetorna404` | Sim | `404` |
| `/api/cursos/{id}` | PUT | `cursos.manter.cursos` | integridade | id divergente no payload | `atualizarCursoComIdDivergenteRetorna422ENaoAlteraBanco` | Sim | `422` |
| `/api/cursos/{id}` | DELETE | `cursos.manter.cursos` | exclusao | curso sem dependencia | `removerCursoValidoRetorna204` | Sim | `204` |
| `/api/cursos/{id}` | DELETE | `cursos.manter.cursos` | inexistente | curso ausente | `removerCursoInexistenteRetorna404` | Sim | `404` |
| `/api/cursos/{id}` | DELETE | `cursos.manter.cursos` | rollback/dependencia | aula, participante e inscricao vinculados | `removerCursoComDependenciasFalhaSemRemocaoParcial` | Sim | `409` |
| `/api/cursos/{cursoId}/arquivos-comprobatorios` | GET | `cursos.manter.cursos` | arquivo associado | listar comprovantes | `listarArquivosComprobatoriosRetornaVinculosDoCurso` | Sim | `200` |
| `/api/cursos/{cursoId}/arquivos-comprobatorios` | POST | `cursos.manter.cursos` | arquivo associado | upload PDF em curso finalizado | `uploadComprobatorioValidoEmCursoFinalizadoCriaArquivoEVinculo` | Sim | `201` |
| `/api/cursos/{cursoId}/arquivos-comprobatorios` | POST | `cursos.manter.cursos` | validacao/rollback | inexistente, nao finalizado, vazio ou mime invalido | `uploadComprobatorioRejeitaCursoInexistenteNaoFinalizadoVazioOuTipoInvalido` | Sim | `404`/`422` |
| `/api/cursos/{cursoId}/arquivos-comprobatorios/{comprovanteId}` | DELETE | `cursos.manter.cursos` | arquivo associado | remove vinculo e arquivo | `removerArquivoComprobatorioRemoveVinculoEArquivo` | Sim | `204` |

Observacoes da Fase 2A:

- O endpoint real administrativo e `/api/cursos`; nao ha `/api/v1/cursos` no contrato atual do service.
- `INSCRICOES_ENCERRADAS` nao existe em `StatusCursoEnum`.
- Detalhes completos estao em `phase-2-api-coverage.md`.

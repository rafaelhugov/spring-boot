# Testes de Seguranca - Modelo de Arquivos

Data: 2026-07-24.

Escopo: backend `cursos-service`, autorizacao de arquivos, documentos gerados e manual administrativo. Nao houve alteracao de frontend, pipeline, frequencia, certificados ou inscricoes fora do necessario para validar propriedade das solicitacoes.

## Regras Funcionais Confirmadas

- `cursos.acesso` permite fluxo interno basico, mas nao concede acesso geral a arquivos.
- `cursos.manter.cursos` e o perfil administrativo do CEAF para arquivos e documentos administrativos.
- Curriculo e foto de ministrante nao sao publicos.
- Solicitante acessa foto/curriculo apenas quando o arquivo esta vinculado a ministrante de sua propria solicitacao.
- Comprovantes de curso e templates de certificado exigem `cursos.manter.cursos`.
- Imagem de cabecalho so e publica quando vinculada a curso `PUBLICADO`.
- Manual administrativo nao aceita token por query string; autenticacao deve vir por `Authorization: Token <token>`.

## Infraestrutura

- `@SpringBootTest` com `MockMvc`.
- Spring Security Test com filtros reais.
- PostgreSQL real via `PostgreSQLContainer("postgres:15-alpine")`.
- Scripts SQL reais do projeto via `@Sql`.
- Banco isolado por teste com `truncate ... restart identity cascade`.

Mocks:

| Mock | Motivo | Interferencia |
|---|---|---|
| `AuthServiceClient` | simular token invalido/valido sem Auth API externo | nao substitui a decisao por recurso |
| `MPVirtualClient` | dependencia externa carregada no contexto | nao participa dos cenarios de arquivo |
| `DocumentosService` | testar autorizacao HTTP sem gerar PDF real | nao interfere no `@PreAuthorize` |
| `ManualPdfService` | testar contrato HTTP do manual sem executar Node/PDF | nao interfere na autenticacao por header |

## Cenarios Automatizados Relevantes

| Classe | Metodo | HTTP | Protecao comprovada |
|---|---|---:|---|
| `ArquivoSecurityIntegrationTest` | `endpointPrivadoSemAutenticacaoRetorna401` | 401 | endpoint privado exige autenticacao |
| `ArquivoSecurityIntegrationTest` | `endpointPrivadoComTokenInvalidoRetorna401` | 401 | token invalido nao entra na camada de negocio |
| `ArquivoSecurityIntegrationTest` | `endpointPrivadoComUsuarioSemAuthorityRetorna403` | 403 | authority obrigatoria |
| `ArquivoSecurityIntegrationTest` | `usuarioADonoDaSolicitacaoDeveBaixarArquivo100` | 200 | dono acessa programacao |
| `ArquivoSecurityIntegrationTest` | `usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA` | 403 | IDOR A/B negado |
| `ArquivoSecurityIntegrationTest` | `solicitanteAcessaCurriculoDeMinistranteDaPropriaSolicitacao` | 200 | solicitante acessa curriculo do proprio pedido |
| `ArquivoSecurityIntegrationTest` | `outroUsuarioNaoBaixaCurriculoDeMinistranteDaSolicitacaoDoUsuarioA` | 403 | IDOR em curriculo negado |
| `ArquivoSecurityIntegrationTest` | `outroUsuarioNaoBaixaFotoDeMinistranteDaSolicitacaoDoUsuarioA` | 403 | IDOR em foto negado |
| `ArquivoSecurityIntegrationTest` | `administradorBaixaFotoDeMinistranteDaSolicitacao` | 200 | CEAF acessa foto |
| `ArquivoSecurityIntegrationTest` | `administradorBaixaCurriculoDeMinistranteDaSolicitacao` | 200 | CEAF acessa curriculo |
| `ArquivoSecurityIntegrationTest` | `usuarioComCursosAcessoNaoBaixaArquivoComprobatorioAdministrativo` | 403 | comprovante exige CEAF |
| `ArquivoSecurityIntegrationTest` | `usuarioComCursosAcessoEAnalisarSolicitacoesNaoBaixaComprovatorioAdministrativo` | 403 | `cursos.analisar.solicitacoes` nao e bypass |
| `ArquivoSecurityIntegrationTest` | `administradorBaixaArquivoComprobatorioComHeadersSeguros` | 200 | CEAF acessa comprovante e headers seguros |
| `ArquivoSecurityIntegrationTest` | `administradorBaixaTemplateCertificado` | 200 | CEAF acessa template |
| `ArquivoSecurityIntegrationTest` | `participanteAutenticadoNaoBaixaTemplateCertificado` | 403 | participante nao baixa template |
| `ArquivoSecurityIntegrationTest` | `semAutenticacaoNaoBaixaTemplateCertificado` | 401 | template exige autenticacao no privado |
| `ArquivoSecurityIntegrationTest` | `arquivoComVinculoAmbiguoRetorna403MesmoParaAdministrador` | 403 | vinculo ambiguo nega por padrao |
| `ArquivoSecurityIntegrationTest` | `imagemCabecalhoDeCursoPublicadoRetorna200NoEndpointPublico` | 200 | publico somente publicado |
| `ArquivoSecurityIntegrationTest` | `templateDeCursoPublicadoNaoRetornaNoEndpointPublico` | 403 | publicacao do curso nao torna template publico |
| `ArquivoSecurityIntegrationTest` | `fotoDeMinistranteNaoRetornaNoEndpointPublico` | 403 | foto nao e publica |
| `ArquivoSecurityIntegrationTest` | `curriculoDeMinistranteNaoRetornaNoEndpointPublico` | 403 | curriculo nao e publico |
| `ArquivoSecurityIntegrationTest` | `gerarDocumentoComTokenInvalidoRetorna401` | 401 | documento rejeita token invalido |
| `ArquivoSecurityIntegrationTest` | `authorityInexistenteNaoGeraDocumentoAdministrativo` | 403 | authority desconhecida nao gera documento |
| `ArquivoSecurityIntegrationTest` | `administradorGeraDocumentoAdministrativo` | 200 | CEAF gera documento |
| `ArquivoSecurityIntegrationTest` | `gerarDocumentoParaCursoInexistenteRetorna404` | 404 | curso inexistente nao vira erro interno |
| `ArquivoSecurityIntegrationTest` | `manualAdministrativoComTokenNaQueryRetorna401` | 401 | query token nao autentica |
| `ArquivoSecurityIntegrationTest` | `manualAdministrativoComHeaderTokenAutorizadoRetorna200` | 200 | header `Authorization: Token` autentica |

## Cenarios A/B

Solicitacao:

- Usuario A `501` e proprietario da solicitacao `10`.
- Ministrante da solicitacao referencia curriculo `101`.
- Usuario A acessa `GET /api/arquivos/101`: `200`.
- Usuario B `502`, com `cursos.acesso`, tenta o mesmo ID: `403`.

Comprovante:

- Arquivo `100` e comprovante do curso `10`.
- Usuario comum com `cursos.acesso` tenta baixar: `403`.
- CEAF com `cursos.manter.cursos` baixa: `200`.

Template:

- Arquivo `100` e template do certificado do curso `10`.
- Participante/solicitante com `cursos.acesso` tenta baixar: `403`.
- CEAF com `cursos.manter.cursos` baixa: `200`.

## Execucao

Comando isolado:

```bash
mvn -q -Dtest=mppb.restapi.cursos.feature.common.integration.ArquivoSecurityIntegrationTest test
```

Resultado real: 51 testes, 0 falhas, 0 erros, 0 ignorados.

## Fase 2A - Cursos API Administrativa

Data: 2026-07-29.

Branch: `test/phase-2a-cursos-api`.

Classe: `CursoApiIntegrationTest`.

Infraestrutura: `@SpringBootTest`, `@AutoConfigureMockMvc`, filtros reais do Spring Security e `PostgreSQLContainer("postgres:15-alpine")`. Nao foi usado `addFilters = false`.

### Autenticacao e autorizacao

| Cenario | Metodo | Resultado |
|---|---|---|
| sem autenticacao em `/api/cursos` | `listarCursosSemAutenticacaoRetorna401` | `401` |
| token invalido via `AuthServiceClient` mockado | `listarCursosComTokenInvalidoRetorna401` | `401` |
| usuario com apenas `cursos.acesso` | `usuarioComApenasCursosAcessoNaoListaCursosAdministrativos` | `403` |
| usuario com `cursos.manter.cursos` | demais cenarios administrativos | `200`/`201`/`204` conforme operacao |

### Entrada e erro sanitizado

| Risco | Cobertura | Resultado |
|---|---|---|
| payload desconhecido com `FAIL_ON_UNKNOWN_PROPERTIES` | `criarCursoComCampoDesconhecidoRetorna400` | `400` sanitizado |
| JSON invalido/deserializacao | `ApiInputExceptionHandler` | `400` generico |
| violacao de integridade em delete | `removerCursoComDependenciasFalhaSemRemocaoParcial` | `409` generico |
| SQL injection em filtro textual | `filtroTituloComSqlInjectionNaoRetornaTodosOsCursos` | `200`, resultado vazio |
| dados sensiveis em erro | asserts contra `cpf`, `token`, `curso.inscricao`, `constraint`, `select`, `SQLException` | nao expostos nos cenarios cobertos |

### Observacoes

- O novo `ApiInputExceptionHandler` tem `@RestControllerAdvice` e `@Order(Ordered.HIGHEST_PRECEDENCE)`, escopado a erros de entrada HTTP e integridade de dados necessarios para esta fase.
- Bean Validation continua tratado pelo handler do `restapi-common`.
- Erros de enum invalido e tipo numerico invalido passam pelo mesmo caminho de deserializacao HTTP; testes adicionais especificos ficam como refinamento de contrato, nao bloqueante para a Fase 2A.

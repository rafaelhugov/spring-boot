# Matriz de Acesso a Arquivos - cursos-service

Data: 2026-07-24.

## Modelo Adotado

Conhecer o ID de `ged.arquivo`, do ministrante, do curso ou da solicitacao nao autoriza acesso. O endpoint generico foi mantido por compatibilidade, mas agora resolve a categoria do arquivo e aplica regra por recurso.

- `cursos.acesso`: permite apenas arquivos da propria solicitacao.
- `cursos.manter.cursos`: permite acesso administrativo CEAF.
- Publico: apenas imagem de cabecalho de curso `PUBLICADO`.
- Vinculo desconhecido, orfao ou ambiguo: negar por padrao.

## Matriz

| Tipo do arquivo | Entidade vinculada | Acesso | Proprietario | Authority | Regra por recurso | Endpoint |
|---|---|---|---|---|---|---|
| Programacao da solicitacao | `SolicitacaoCurso.idArquivoProgramacaoGed` | privado individual | `SolicitacaoCurso.idServidorInteressado` | dono com `cursos.acesso`; CEAF com `cursos.manter.cursos` | dono acessa; outro usuario recebe `403`; CEAF acessa | `GET /api/arquivos/{id}` |
| Foto de ministrante em solicitacao | `SolicitacaoMinistrante.idFoto` | privado individual | solicitante da `SolicitacaoCurso` | dono com `cursos.acesso`; CEAF com `cursos.manter.cursos` | valida usuario, solicitacao, ministrante da solicitacao e arquivo | `GET /api/arquivos/{id}` |
| Curriculo de ministrante em solicitacao | `SolicitacaoMinistrante.idCurriculo` | privado individual | solicitante da `SolicitacaoCurso` | dono com `cursos.acesso`; CEAF com `cursos.manter.cursos` | mesma regra da foto; ID conhecido nao basta | `GET /api/arquivos/{id}` |
| Arquivo de ministrante cadastrado | `Ministrante.idFoto`/`idCurriculo` sem vinculo de solicitacao | administrativo | cadastro de ministrante | `cursos.manter.cursos` | usuario comum recebe `403`; CEAF acessa | `GET /api/arquivos/{id}` |
| Arquivo comprobatorio de curso | `CursoArquivoComprobatorio.arquivo.id` | administrativo | curso | `cursos.manter.cursos` | `cursos.acesso` e `cursos.analisar.solicitacoes` nao liberam | `GET /api/arquivos/{id}`; `POST /api/cursos/{cursoId}/arquivos-comprobatorios` |
| Imagem de cabecalho publicada | `Curso.headerImageId` com `status = PUBLICADO` | publico | curso publicado | nenhuma no endpoint publico | somente imagem de cabecalho fica publica | `GET /api/public/arquivos/{id}` |
| Imagem de cabecalho nao publicada | `Curso.headerImageId` sem `PUBLICADO` | administrativo | curso | `cursos.manter.cursos` | publico recebe `403`; usuario comum no privado recebe `403`; CEAF acessa | `GET /api/arquivos/{id}` |
| Template de certificado | `Curso.certificateTemplateId` | administrativo | curso | `cursos.manter.cursos` | participante, solicitante e usuario comum recebem `403` | `GET /api/arquivos/{id}` |
| Documento administrativo gerado | `DocumentosService` | administrativo | curso | `cursos.manter.cursos` | sem auth `401`; invalido `401`; comum `403`; CEAF `200`; inexistente `404` | `POST /api/cursos/{id}/documentos/gerar` |
| Manual solicitante | PDF de manual | publico | nenhum | nenhuma | permanece publico | `GET /api/manuais/pdf/solicitante` |
| Manual administrativo | PDF de manual | administrativo | perfil CEAF | `cursos.manter.cursos` | somente `Authorization: Token <token>`; query token recebe `401` | `GET /api/manuais/pdf/administrativo` |
| Arquivo orfao | `ged.arquivo` sem vinculo conhecido | bloqueado | nenhum | nenhuma libera | privado autenticado `403`; publico `403`/`404` conforme existencia | `GET /api/arquivos/{id}` |
| Arquivo ambiguo | mais de uma finalidade incompativel | bloqueado | indefinido | nenhuma libera | `403` inclusive para CEAF ate corrigir dados | `GET /api/arquivos/{id}` |

## Evidencia de Propriedade

O usuario autenticado e obtido em `ArquivoService` por `SecurityUtils.getAuthenticationToken().map(auth -> auth.getUsuario().getId())`.

Consultas por propriedade:

- `ArquivoRepository.existsSolicitacaoProgramacaoReferenceForServidor(arquivoId, servidorId)`.
- `ArquivoRepository.existsSolicitacaoMinistranteReferenceForServidor(arquivoId, servidorId)`.

Consultas por categoria:

- `existsCursoHeaderImageReference`.
- `existsCursoCertificateTemplateReference`.
- `existsArquivoComprobatorioReference`.
- `existsMinistranteReference`.
- `existsSolicitacaoMinistranteReference`.
- `existsSolicitacaoProgramacaoReference`.

## Decisoes

- `cursos.analisar.solicitacoes` nao foi tratado como authority administrativa para arquivos.
- `ged.arquivo` nao recebeu owner/finalidade para evitar impacto em outros sistemas.
- Upload generico permanece por compatibilidade, mas arquivo orfao nao pode ser baixado.

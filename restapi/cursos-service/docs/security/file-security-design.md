# File Security Design - cursos-service

Data: 2026-07-24.

## Objetivo

Definir e comprovar o modelo de autorizacao de arquivos do `cursos-service`, evitando IDOR em downloads por ID numerico.

## Principio

O ID de `ged.arquivo` identifica o blob, mas nao autoriza acesso. A autorizacao deve ser derivada do recurso de negocio que referencia o arquivo.

## Modelo atual apos correcao

| Endpoint | Acesso | Regra aplicada |
|---|---|---|
| `POST /api/arquivos/upload` | `cursos.acesso` ou `cursos.manter.cursos` | valida arquivo e persiste no GED; continua generico por compatibilidade |
| `GET /api/arquivos/{id}` | `cursos.acesso` ou `cursos.manter.cursos` | identifica vinculos conhecidos; admin acessa recursos privados; usuario comum acessa apenas arquivos da propria solicitacao |
| `GET /api/public/arquivos/{id}` | publico | somente `Curso.headerImageId` quando `Curso.status = PUBLICADO` |
| `POST /api/cursos/{cursoId}/arquivos-comprobatorios` | `cursos.manter.cursos` | upload + associacao contextual com curso |
| `POST /api/cursos/{id}/documentos/gerar` | `cursos.manter.cursos` | geracao administrativa de PDF |
| `GET /api/manuais/pdf/administrativo` | header `Authorization: Token <token>` com `cursos.manter.cursos` | token em query string removido do backend |

## Resolucao de acesso privado

Fluxo em `ArquivoService.buscarArquivoPrivadoAutorizado`:

1. Busca `ged.arquivo`; se nao existir, retorna `404`.
2. Verifica se existe vinculo conhecido em curso, comprovante, ministrante, solicitacao de ministrante ou solicitacao de curso.
3. Se nao houver vinculo, retorna `403`.
4. Classifica a finalidade do arquivo por vinculo: cabecalho, template, comprovante, ministrante cadastrado, ministrante de solicitacao ou programacao de solicitacao.
5. Se houver mais de uma finalidade incompativel, retorna `403`.
6. Se o usuario tem `cursos.manter.cursos`, libera como acesso administrativo.
7. Se for usuario comum, obtem `usuario.id` do `TokenAuthentication`.
8. Libera apenas quando o arquivo pertence a uma solicitacao cujo `idServidorInteressado` e o usuario autenticado.
9. Nega por padrao quando nao houver regra inequivoca.

## Upload generico

O upload generico permanece por compatibilidade com o frontend atual. Limitacoes:

- nao registra owner em `ged.arquivo`;
- nao registra finalidade/categoria;
- pode criar arquivo temporariamente orfao;
- tipo permitido nao varia por fluxo.

Compensacao implementada:

- arquivo orfao nao pode ser baixado;
- usuario comum nao consegue baixar arquivo administrativo apenas por ID;
- associacoes administrativas continuam protegidas por `cursos.manter.cursos`.

Recomendacao: migrar para uploads contextualizados, conforme `../api/file-migration-plan.md`.

## Validacao de upload

`ArquivoSecurityUtils.validateUpload`:

- tamanho maximo: 5 MB;
- extensoes permitidas: `pdf`, `png`, `jpg`, `jpeg`;
- rejeita arquivo vazio;
- rejeita extensao dupla;
- rejeita path traversal, separadores e caracteres de controle;
- valida `Content-Type` declarado contra extensao;
- valida assinatura basica de PDF, PNG e JPEG;
- sanitiza o nome persistido.

## Download seguro

`ArquivoSecurityUtils.secureDownloadResponse`:

- `Content-Type` definido por allowlist;
- `Content-Disposition: attachment` com nome sanitizado;
- `X-Content-Type-Options: nosniff`;
- `Cache-Control: no-store` para privado;
- `Cache-Control: public, max-age=3600` para publico permitido;
- nao expoe caminho fisico.

## Consumidores atuais

| Consumidor | Uso | Impacto da nova regra |
|---|---|---|
| `arquivoClient.upload` | upload generico em solicitacao, curso e ministrante | continua funcionando para usuarios com `cursos.acesso` ou `cursos.manter.cursos` |
| `arquivoClient.downloadAsBlob` | download privado | usuario comum passa a receber `403` para arquivos administrativos |
| `arquivoClient.getDownloadUrl` | href/img privado | pode quebrar visualizacao se usado fora da propria solicitacao para usuario comum |
| `arquivoClient.getPublicDownloadUrl` | imagem publica | continua funcionando apenas para curso publicado |
| `ManualTopBar.vue` | abre PDF administrativo via `apiClient` e `blob` | compativel com backend que exige header |
| `ManualAdminView.vue` | usa `apiClient` com header | compativel |

## Exemplo IDOR negado

Pre-condicao:

- Usuario A: servidor `501`.
- Usuario B: servidor `502`.
- Arquivo `100`: vinculado a `SolicitacaoCurso.id = 10`, `idServidorInteressado = 501`.

Tentativa:

```http
GET /api/arquivos/100
Authorization: Token <token-usuario-b>
```

Resultado:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json
```

Teste: `ArquivoSecurityIntegrationTest.usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA`.

## Teste de eficacia

| Protecao | Teste | Alteracao indevida que faz falhar | Comando |
|---|---|---|---|
| Auth obrigatoria no privado | `endpointPrivadoSemAutenticacaoRetorna401` | tornar `/api/arquivos/**` publico | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#endpointPrivadoSemAutenticacaoRetorna401 test` |
| Authority obrigatoria | `endpointPrivadoComUsuarioSemAuthorityRetorna403` | remover `@PreAuthorize` | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#endpointPrivadoComUsuarioSemAuthorityRetorna403 test` |
| Propriedade da solicitacao | `usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA` | remover verificacao de `idServidorInteressado` | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA test` |
| Comprovante admin | `usuarioComCursosAcessoNaoBaixaArquivoComprobatorioAdministrativo` | permitir comprovante apenas por vinculo | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#usuarioComCursosAcessoNaoBaixaArquivoComprobatorioAdministrativo test` |
| Publicacao explicita | `imagemDeCursoNaoPublicadoNaoRetornaNoEndpointPublico` | remover `status = PUBLICADO` | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#imagemDeCursoNaoPublicadoNaoRetornaNoEndpointPublico test` |
| Documento admin | `usuarioComCursosAcessoNaoGeraDocumentoAdministrativo` | remover `@PreAuthorize` do documento | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#usuarioComCursosAcessoNaoGeraDocumentoAdministrativo test` |
| Template admin | `participanteAutenticadoNaoBaixaTemplateCertificado` | permitir `cursos.acesso` em template | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#participanteAutenticadoNaoBaixaTemplateCertificado test` |
| Manual sem query token | `manualAdministrativoComTokenNaQueryRetorna401` | voltar a aceitar token na URL | `mvn -q -Dtest=ArquivoSecurityIntegrationTest#manualAdministrativoComTokenNaQueryRetorna401 test` |

## Componentes mockados nos testes integrados

| Mock | Motivo | Interferencia |
|---|---|---|
| `AuthServiceClient` | evitar Auth API externo e simular token invalido | nao substitui a decisao por recurso; usuarios usam `TokenAuthentication` real no contexto |
| `MPVirtualClient` | cliente externo carregado pelo contexto | nao participa dos cenarios de arquivo |
| `DocumentosService` | validar autorizacao HTTP do controller sem gerar PDF real | nao interfere na regra de `@PreAuthorize` testada |
| `ManualPdfService` | validar contrato HTTP do manual sem executar Node/PDF | nao interfere na autenticacao por header |

## Riscos pendentes

- `ged.arquivo` ainda nao registra owner/finalidade.
- ID numerico permanece no contrato por compatibilidade.
- Upload contextualizado ainda nao foi criado.
- Validacao manual do novo fluxo do `ManualTopBar.vue` deve confirmar ausencia de token em URL/historico.
- Endpoints contextualizados ainda devem substituir gradualmente o endpoint generico.

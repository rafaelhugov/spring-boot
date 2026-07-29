# Testes de API

## Contratos por endpoint

| Endpoint | Sucesso | Invalidos | Auth/Authz | Seguranca especifica |
|---|---|---|---|---|
| `GET /api/public/cursos` | lista paginada | filtros longos, pagina negativa | publico | rate limit, headers cache |
| `GET /api/public/cursos/{hash}` | curso publico | hash inexistente/malformado | publico | IDOR por hash |
| `POST /api/public/inscricoes/{hash}/externo` | 201/200 inscricao | CPF/email inválidos, XSS, JSON extra | publico | rate limit, SQLi, XSS persistente |
| `GET /api/public/arquivos/{id}` | download | id inexistente | publico controlado | IDOR, content-disposition, MIME sniff |
| `GET /api/public/frequencia/aula/{aulaId}` | dados chamada | aula inexistente | publico | enumeration/rate limit |
| `POST /api/public/frequencia/aula/{aulaId}/registrar` | registra presenca | CPF invalido, fora horario | publico | concorrencia, replay, rate limit |
| `GET /api/cursos` | pagina cursos | sort/filtro invalido | `cursos.manter.cursos` | SQLi em filtros |
| `POST /api/cursos` | cria curso | payload invalido/extra | `cursos.manter.cursos` | CSRF se cookie for adotado |
| `PUT /api/cursos/{id}` | atualiza | id inexistente/divergente | `cursos.manter.cursos` | IDOR |
| `DELETE /api/cursos/{id}` | remove | em uso/inexistente | `cursos.manter.cursos` | IDOR |
| `POST /api/cursos/{id}/certificado-template` | upload imagem | >5MB, MIME errado | `cursos.manter.cursos` | malware/polyglot |
| `GET /api/cursos/{cursoId}/participantes/export` | Excel | filtros invalidos | `cursos.manter.cursos` | CSV/Excel injection |
| `POST /api/cursos/{cursoId}/documentos/*` | PDF | payload incompleto | `cursos.manter.cursos` | SSRF/template injection |
| `GET /api/lookup/*` | dominios | indisponibilidade DB | `cursos.acesso` | cache/header |
| `POST /api/solicitacoes` | cria solicitacao | listas aninhadas invalidas | `cursos.acesso` | XSS/SQLi |
| `POST /api/solicitacoes/{id}/protocolar` | protocolo | status invalido | `cursos.manter.cursos` | idempotencia, falha MPVirtual |
| `POST /api/solicitacoes/{id}/converter` | curso criado | ja convertida | `cursos.manter.cursos` | transacao |
| `POST /api/arquivos/upload` | id GED | vazio/>5MB | autenticado | upload seguro |

## Payloads maliciosos padrao

- SQLi: `' OR '1'='1`, `1; DROP TABLE cursos.curso`.
- XSS: `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`.
- Path traversal em nome de arquivo: `../../shell.jsp`.
- Excel injection: `=HYPERLINK("http://evil")`.
- JSON extra: campos desconhecidos devem falhar onde `FAIL_ON_UNKNOWN_PROPERTIES` se aplica, exceto DTOs explicitamente anotados com ignore.


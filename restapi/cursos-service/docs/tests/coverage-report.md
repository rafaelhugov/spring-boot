# Relatorio de Cobertura

Data de medicao: 2026-07-24.

Escopo: backend `spring-boot/restapi/cursos-service`, com foco nas regras funcionais confirmadas para arquivos, documentos e manual administrativo. Nao houve objetivo de aumentar cobertura global de forma indiscriminada.

## Comandos

Executados:

| Comando | Total | Falhas | Erros | Ignorados | Resultado |
|---|---:|---:|---:|---:|---|
| `mvn -q -pl restapi-common install -DskipTests` | n/a | 0 | 0 | 0 | passou |
| `mvn -q -Dtest=mppb.restapi.cursos.feature.common.service.ArquivoServiceTest test` | 11 | 0 | 0 | 0 | passou |
| `mvn -q -Dtest=mppb.restapi.cursos.feature.common.integration.ArquivoSecurityIntegrationTest test` | 51 | 0 | 0 | 0 | passou |
| `mvn -q clean test` | 114 | 0 | 0 | 0 | passou |
| `mvn -q clean verify` | 114 | 0 | 0 | 0 | passou |

Observacoes operacionais:

- Testcontainers exigiu acesso ao Docker fora do sandbox.
- A instalacao local do `restapi-common` foi necessaria para o `cursos-service` carregar o provider sem log de token.
- O Surefire registrou aviso de encerramento lento do fork apos shutdown do Testcontainers, mas todos os comandos terminaram com exit code 0 e XMLs com 0 falhas/erros.
- Relatorio HTML do JaCoCo: `spring-boot/restapi/cursos-service/target/site/jacoco/index.html`.

## Cobertura

Cobertura anterior conhecida antes desta tarefa:

| Metrica | Cobertura anterior |
|---|---:|
| Instrucoes | 17,79% (2968/16683) |
| Branches | 11,76% (186/1582) |
| Linhas | 22,12% (742/3355) |
| Metodos | 15,09% (210/1392) |

Cobertura apos `mvn -q clean verify`:

| Metrica | Cobertura nova |
|---|---:|
| Instrucoes | 18,34% (3068/16726) |
| Branches | 12,31% (195/1584) |
| Linhas | 22,55% (758/3361) |
| Metodos | 15,45% (215/1392) |

Interpretacao:

- Aumento localizado em instrucoes, linhas, branches e metodos.
- O maior ganho de valor esta nos cenarios de autorizacao negativa, nao no percentual global.

## Status HTTP Comprovados

| Status | Cenarios |
|---|---|
| `200` | CEAF baixa comprovante, template, foto/curriculo de ministrante; solicitante baixa arquivo da propria solicitacao; imagem publicada publica; manual administrativo via header |
| `401` | endpoint privado sem auth; token invalido; upload sem auth; documento sem auth/token invalido; template privado sem auth |
| `403` | usuario sem authority; authority inexistente; Usuario B acessando arquivo de A; comprovante/template por `cursos.acesso`; publico tentando foto/curriculo/template; arquivo ambiguo |
| `404` | arquivo inexistente privado/publico; documento de curso inexistente |

## Lacunas

- Token expirado real depende de contrato do Auth API externo.
- Upload generico ainda nao registra owner/finalidade.
- Rate limit de endpoints publicos permanece pendente.
- Testes de concorrencia de upload/download permanecem pendentes.
- Frontend do manual administrativo ajustado para usar `apiClient` com header e resposta `blob`.

## Fase 2A - Cursos API

Data de medicao: 2026-07-29.

Escopo: API administrativa de Cursos no `cursos-service`, branch `test/phase-2a-cursos-api`, commit base `cfbd52b06c6aa7bad7cbbc75c61b07ebb2f6296f`.

### Comandos

| Comando | Exit code | Total | Falhas | Erros | Ignorados | Resultado |
|---|---:|---:|---:|---:|---:|---|
| `mvn -q test` | 0 | 141 | 0 | 0 | 0 | passou |
| `mvn -q verify` | 0 | 141 | 0 | 0 | 0 | passou |
| `mvn -q clean -Dtest=CursoApiIntegrationTest test` | 0 | 24 | 0 | 0 | 0 | passou |
| `mvn -pl restapi-common,cursos-service -am clean verify` | 0 | 141 em `cursos-service`; 2 em `restapi-common` | 0 | 0 | 0 | passou |

Observacao: o baseline real da branch era 117 testes no `cursos-service`. O numero 119 anterior veio de contagem textual que incluiu `@Testcontainers` e `@TestInstance`.

### Cobertura global

| Metrica | Antes | Depois | Variacao |
|---|---:|---:|---:|
| Instrucoes | 18,33% | 22,02% | +3,69 p.p. |
| Branches | 12,23% | 14,44% | +2,21 p.p. |
| Linhas | 22,52% | 26,27% | +3,75 p.p. |
| Metodos | 27,14% | 32,67% | +5,53 p.p. |

Numeradores finais do JaCoCo:

| Metrica | Cobertura nova |
|---|---:|
| Instrucoes | 3701/16809, 22,02% |
| Branches | 231/1600, 14,44% |
| Linhas | 887/3377, 26,27% |
| Metodos | 196/600, 32,67% |

### Cobertura por classe

| Classe | Linhas | Branches | Metodos |
|---|---:|---:|---:|
| `mppb.restapi.cursos.ApiInputExceptionHandler` | 11/11, 100,00% | n/a | 3/3, 100,00% |
| `mppb.restapi.cursos.feature.curso.controller.CursoController` | 20/87, 22,99% | 10/12, 83,33% | 11/34, 32,35% |
| `mppb.restapi.cursos.feature.curso.service.CursoService` | 75/201, 37,31% | 14/90, 15,56% | 15/37, 40,54% |
| `CursoRepository` | nao aparece como classe instrumentada no CSV do JaCoCo | n/a | n/a |
| `CursoDto` | nao aparece como classe instrumentada no CSV do JaCoCo | n/a | n/a |

### Surefire

O aviso `Surefire is going to kill self fork JVM...` ocorreu no shutdown apos `System.exit(0)`. A classificacao e compreendido e nao bloqueante: exit code 0, XMLs completos, 0 falhas/erros, sem impacto funcional comprovado. `docker ps` mostrou apenas containers persistentes de desenvolvimento, sem Testcontainers remanescentes.

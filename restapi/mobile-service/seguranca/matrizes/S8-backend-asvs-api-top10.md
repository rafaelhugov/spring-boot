# S8 - Matriz backend ASVS/API Security Top 10

Data: 2026-09-24

Escopo: `spring-boot/restapi/mobile-service` e `spring-boot/restapi/restapi-common`

Branch: `seguranca/s8-matrizes-rastreabilidade`

## Criterios

- **Coberto**: existe implementacao, teste automatizado ou gate versionado.
- **Parcial**: controle implementado, mas falta teste negativo, DAST, carga ou homologacao.
- **Bloqueado**: depende de homologacao, runner, ferramenta externa ou credencial institucional.
- **N/A**: nao aplicavel ao endpoint/fluxo atual, com justificativa.

## Matriz API Top 10 2023 / ASVS aplicavel

| ID | Referencia | Aplicabilidade | Status | Evidencia | Risco residual / proxima acao |
|---|---|---|---|---|---|
| API-01 | API1 BOLA | Carteira, contracheque, pagamentos, RH, designacoes e notificacoes por usuario | Parcial | Spring Security centralizado S3; controllers usam matricula autenticada; testes `*ControllerTest` | Criar suite negativa multiusuario com IDs de outro usuario. |
| API-02 | API2 Broken Authentication | Todos endpoints privados | Coberto parcial | Bearer no header; S2 removeu token em URL; `security_regression.sh` | Adicionar testes de JWT malformado/expirado/issuer/audience quando provedor permitir. |
| API-03 | API3 BOPLA | DTOs de operacao RH, notificacao e respostas sensiveis | Parcial | `OperacaoSolicitacaoRequestDtoTest`; mapeadores/testes de servico | Testar payload com propriedades extras e resposta minima por endpoint. |
| API-04 | API4 Resource Consumption | Consulta publica, captcha, PDFs, endpoints autenticados | Parcial | Captcha em consulta publica; testes unitarios de repositorios | Adicionar rate limit, limites de tamanho e teste de abuso controlado. |
| API-05 | API5 BFLA | Rotas administrativas/diagnostico/operacoes | Parcial | Spring Security; rotas privadas fora de `PUBLIC_URLS`; `security_regression.sh` | Matriz de papeis/claims ainda pendente. |
| API-06 | API6 Sensitive Business Flows | Contracheque, pagamentos, carteira, concessoes, FCM | Parcial | Testes de controllers/services; logs sanitizados em FCM | Testes de automacao/enumeração em homologacao. |
| API-07 | API7 SSRF | URLs controladas por usuario | N/A parcial | Nenhum endpoint publico atual recebe URL arbitraria para chamada server-side | Reavaliar se novos endpoints aceitarem URL/arquivo remoto. |
| API-08 | API8 Misconfiguration | Profiles, defaults, CORS, headers, logs, Actuator | Coberto parcial | S4/S6; `ValidadorConfiguracaoSeguraTest`; `security_regression.sh` | Validar configuracao efetiva prod-like em homologacao. |
| API-09 | API9 Inventory Management | Inventario de endpoints | Coberto parcial | S1 `S1-backend-controllers.md`; pipeline/gates S7 | Manter inventario atualizado a cada endpoint novo. |
| API-10 | API10 Unsafe Consumption of APIs | Auth API, MPVirtual, JCR, Firebase | Parcial | Feign clients; testes de repositorios/services; S6 segredos | Testar timeout, resposta maliciosa e indisponibilidade por integracao. |

## Matriz ASVS resumida por dominio

| Dominio | Aplicabilidade | Status | Evidencia | Lacuna |
|---|---|---|---|---|
| Arquitetura e threat model | API mobile com dados funcionais/sensiveis | Parcial | S1 threat model inicial | Expandir por fluxo sensivel em S8-T13. |
| Autenticacao | Bearer token centralizado | Coberto parcial | S2/S3, testes de controllers | Suite negativa completa de tokens. |
| Sessao | Sessao controlada por token/API auth externa | Parcial | S5 remove senha local e evita token em URL | Revogacao/refresh dependem do provedor. |
| Controle de acesso | Endpoints privados centralizados | Coberto parcial | S3 `SecurityConfig`, gates S7 | BOLA/BFLA multiusuario pendente. |
| Validacao de entrada | DTOs e parametros de endpoints | Parcial | Testes DTO/repositorios | Fuzz/payload extra pendente. |
| Criptografia e segredos | Sem defaults inseguros; segredos externos | Coberto parcial | S6, secret scan S7 | Rotacao real e cofre institucional fora do repo. |
| Erros e logging | Sanitizacao de logs sensiveis | Coberto parcial | Testes FCM/logging e revisoes S4/S6 | Auditoria/eventos de seguranca formal pendente. |
| Protecao de dados | Contracheque, carteira, pagamentos, RH | Parcial | Autorizacao central; testes de servico | Cache/header/no-store e DAST pendentes. |
| Comunicacao | HTTPS/gateway esperado; Authorization no header | Parcial | S2/S4; sem token em URL | TLS/proxy real em homologacao. |
| Configuracao | Fail-fast e profiles | Coberto parcial | `ValidadorConfiguracaoSeguraTest`; S4/S6 | Validacao prod-like operacional. |
| APIs e servicos | Controllers e clients externos | Parcial | Inventario S1; testes de repositorio/service | DAST autenticado e testes de integracao adversarial. |

## Decisao tecnica desta etapa

Nao ha bloqueador critico novo identificado na matriz local. A prontidao final depende de suite negativa multiusuario, DAST autenticado em homologacao e validacao de configuracao efetiva de producao/homologacao.

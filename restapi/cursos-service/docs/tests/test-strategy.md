# Estrategia de Testes - Cursos

Data da analise: 2026-07-23.

## Escopo analisado

- Backend: `spring-boot/restapi/cursos-service`, Spring Boot 3.5.6, Java 21, JPA/PostgreSQL, Spring Security, OpenFeign, JasperReports, Thymeleaf/OpenHTMLToPDF, uploads GED e geracao de PDFs.
- Frontend: `mppb-vue/cursos-web`, Vue 3, Vite 7, Pinia, Vue Router, Vuetify, Axios.
- Banco: entidades JPA nos schemas `cursos`, `ged` e dependencias de views/tabelas corporativas via `restapi-common`.
- Autenticacao/autorizacao: frontend usa Auth API externa e envia `Authorization: Token <token>`; backend usa `@PreAuthorize` com autoridades `cursos.acesso`, `cursos.dashboard`, `cursos.cadastros`, `cursos.manter.cursos`.
- Integrações: Auth API MPPB, MPVirtual/protocolo, SMTP/email, GED/arquivo, RH/servidores, PDF engine Node para manuais.

## Piramide proposta

- Unitarios: services, validators, formatters, composables puros, conversores e regras de dominio.
- Integracao: repositories com Testcontainers PostgreSQL, controllers com MockMvc/Spring Security, geracao PDF com templates reais.
- API: contratos HTTP por endpoint, validacao, seguranca e payloads de erro.
- E2E: Playwright ou Cypress cobrindo fluxos completos por perfil.
- Regressao: checklist por release e smoke automatizado.
- Segurança: OWASP API Top 10, upload/download, IDOR, headers, CSRF/XSS, SQL injection, rate limit.
- Performance: k6/JMeter/Gatling para listagens, inscricoes, frequencia e downloads.

## Gates de qualidade

- Pull request: `npm run test`, `npm run test:coverage`, `mvn test`.
- Release candidate: `mvn verify`, E2E smoke, testes de API autenticados, SAST/dependency audit.
- Meta inicial: backend 40% linhas, frontend 25% linhas.
- Meta de maturidade: backend 80% linhas/70% branches em services/controllers; frontend 70% linhas em componentes e composables criticos.

## Ferramentas configuradas

- Backend: JUnit 5, Mockito, Spring Boot Test, Testcontainers ja presentes; JaCoCo adicionado.
- Frontend: Vitest, Vue Test Utils, jsdom e cobertura V8 adicionados.
- Fixtures/factories: `src/test/fixtures/curso.fixture.js` e `CursoTestDataFactory`.


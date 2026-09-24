# S7 - Pipeline e gates de seguranca do backend

## Escopo

Branch: `seguranca/s7-pipeline-security-gates`

Repositorio: `spring-boot`

Servico coberto: `restapi/mobile-service`, com dependencia direta em `restapi/restapi-common`.

## Gates adicionados

- `test:mobile-service`: executa `mvn -q -pl restapi-common,mobile-service test`.
- `security:secrets`: falha quando arquivos sensiveis ou chaves privadas aparecem versionados.
- `security:regression`: falha quando regressam controles ja corrigidos nas fases S2, S3, S4 e S6.
- `build:mobile-service`: empacota o servico depois dos testes e gates de seguranca.
- `evidence:dependencies`: gera arvore de dependencias Maven como evidencia inicial de SCA/SBOM.

## Politica inicial

- Segredo confirmado em arquivo versionado bloqueia o merge.
- Regressao de token em query string, esquema `Token` legado, endpoint sensivel publico ou fallback inseguro de producao bloqueia o merge.
- Evidencia de dependencias e mantida como artefato por 1 mes para apoiar a classificacao de CVEs.

## Pendencias planejadas

- Evoluir a evidencia Maven para SBOM CycloneDX formal.
- Integrar SAST dedicado quando a ferramenta corporativa estiver definida.
- Integrar DAST apenas contra homologacao autorizada.

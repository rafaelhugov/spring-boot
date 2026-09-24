# S8 - Evidencia de regressao backend

Data: 2026-09-24

Escopo: `spring-boot/restapi/mobile-service` e `spring-boot/restapi/restapi-common`

## Comandos previstos

```bash
bash restapi/mobile-service/scripts/security/secret_scan.sh
bash restapi/mobile-service/scripts/security/security_regression.sh
mvn -q -pl restapi-common,mobile-service test
mvn -q -pl restapi-common,mobile-service -DskipTests package
```

## Resultado

| Comando | Resultado |
|---|---|
| `bash restapi/mobile-service/scripts/security/secret_scan.sh` | Passou |
| `bash restapi/mobile-service/scripts/security/security_regression.sh` | Passou |
| `mvn -q -pl restapi-common,mobile-service test` | Passou |
| `mvn -q -pl restapi-common,mobile-service -DskipTests package` | Passou |

## Ajuste de gate

Durante a regressao, a regra de secret scan foi ajustada para excluir `restapi/mobile-service/scripts/security/**` da busca de conteudo, evitando que o script encontre o proprio regex. A varredura segue cobrindo `restapi/mobile-service`, `restapi/restapi-common` e `.gitlab-ci.yml`.

## Observacoes

- O pipeline GitLab foi versionado na S7, mas a execucao automatica ficou bloqueada por ausencia de runner ativo no projeto.
- A validacao desta etapa sera local ate a infraestrutura de CI/CD estar disponivel.

# S4 - Hardening de produção backend

Data: 2026-09-24

Branch: `seguranca/s4-hardening-producao`

## Objetivo

Evitar inicialização silenciosa do `mobile-service` com perfil, credenciais ou parâmetros inseguros em produção.

## Alterações executadas

- `spring.profiles.active` deixou de assumir `dev` por default.
- `spring.jpa.show-sql` passou a ser `false` por default.
- Defaults inseguros removidos de:
  - `spring.datasource.url`
  - `spring.datasource.username`
  - `spring.datasource.password`
  - `mpvirtual.recursos-humanos.base-url`
  - `jcr.url`
  - `jcr.user`
  - `jcr.password`
- Novo validador `ValidadorConfiguracaoSegura` executa fail-fast no startup.

## Regras de fail-fast

- Startup falha sem `SPRING_PROFILES_ACTIVE` explícito.
- `prod` não pode ser combinado com `insecure`.
- `prod` não pode ser combinado com `trace`.
- `spring.jpa.show-sql=true` falha em `prod`.
- `hibernate.format_sql=true` falha em `prod`.
- Configurações críticas ausentes falham em `prod`.
- `jcr.user=admin`, `jcr.password=admin` e `spring.datasource.password=senha` falham em `prod`.
- URLs locais em datasource e URL pública do Wallet falham em `prod`.
- CORS com credenciais e origem wildcard falha em `prod`.

## Validação

Comando executado:

```bash
mvn -q -pl restapi-common,mobile-service test
```

Resultado: aprovado.

Varredura executada:

```bash
rg -n "SPRING_PROFILES_ACTIVE:dev|JPA_SHOW_SQL:true|PG_PASSWORD:senha|JCR_USER:admin|JCR_PASSWORD:admin|MPVIRTUAL_RECURSOS_HUMANOS_BASE_URL:http://" src/main/resources/application.yml src/main/java src/test/java
```

Resultado: sem ocorrências.

## Testes adicionados

- `ValidadorConfiguracaoSeguraTest`
  - falha sem perfil explícito;
  - falha com `prod,insecure`;
  - falha com SQL log ativo em produção;
  - falha com credencial default em produção;
  - aceita configuração de produção explícita.

## Rollback

Reverter o commit da S4 restaura os defaults anteriores e remove o validador de startup.

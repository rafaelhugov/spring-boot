# S6 - Gestão de segredos backend

Data: 2026-09-24

Branch: `seguranca/s6-gestao-segredos`

## Arquivos sensíveis locais

Arquivos locais encontrados e não rastreados:

- `.env.dev`
- `google-credentials.json`
- SQLs locais de pendência de produção ignorados desde S1

Esses arquivos devem ficar fora do Git e ser armazenados em cofre/CI/deploy com acesso mínimo.

## Alterações executadas

- `.gitignore` do repositório `spring-boot` passou a bloquear padrões globais para:
  - `.env`, `.env.*`, `*.env`;
  - `.docker/*.env`;
  - `*credentials*.json`;
  - `*service-account*.json`;
  - `google-credentials.json`;
  - `*.jks`, `*.p12`, `*.pem`, `*.p8`.
- `.gitignore` do `mobile-service` reforçado com padrões de service account e chaves.
- `JcrService` não possui mais fallback hardcoded para URL, usuário ou senha do JCR.

## Secret scan

Working tree:

```bash
rg -n "AIza|-----BEGIN|private_key|client_email|password\s*[:=]|senha\s*[:=]|token\s*[:=]|JCR_PASSWORD|PG_PASSWORD|GOOGLE_APPLICATION_CREDENTIALS|google-credentials|service_account|keystore|storePassword|keyPassword" . -S -g '!target/**' -g '!**/target/**' -g '!**/.gradle/**'
```

Resultado: sem segredo técnico confirmado no código rastreado. Achados classificados:

- Referências a variáveis de ambiente obrigatórias.
- Documentação histórica de riscos já tratados.
- FCM/captcha tokens de negócio ou testes, não segredos técnicos.

Histórico Git:

```bash
git log --all --name-only --pretty=format: | rg -n "(^|/)(\.env|.*\.env|.*key.*|.*keystore.*|.*credentials.*|.*service-account.*|google-credentials\.json|google-services\.json|GoogleService-Info\.plist|.*\.jks|.*\.p12|.*\.pem|.*\.p8)$" | sort -u
```

Resultado:

- `restapi/.docker/api.env`

Classificação: arquivo de ambiente no histórico. Exige revisão/rotação operacional das credenciais que possam ter passado por esse arquivo.

## Pendências operacionais

- Verificar se credenciais que existiram em `restapi/.docker/api.env` foram rotacionadas.
- Armazenar `google-credentials.json`/service account em cofre ou secret manager, não no filesystem versionado.
- Em S7, adicionar secret scanning automático no pipeline.

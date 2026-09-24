#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

failed=0

report() {
  echo "S7 security regression: $*"
  failed=1
}

if git grep -n -E 'RequestParam[^(]*\([^)]*"token"|ResolvedorTokenAutenticacao|Authorization[[:space:]]*:[[:space:]]*"Token ' -- restapi/mobile-service/src/main/java restapi/restapi-common/src/main/java; then
  report "regressao de token em query string ou esquema Token legado"
fi

if git grep -n -E 'JPA_SHOW_SQL:true|PG_PASSWORD:senha|JCR_PASSWORD:admin|jcr\.password:admin|jcr\.user:admin' -- restapi/mobile-service/src/main/java restapi/mobile-service/src/main/resources; then
  report "fallback inseguro de producao encontrado"
fi

security_config="restapi/restapi-common/src/main/java/mppb/restapi/common/infrastructure/config/SecurityConfig.java"
if git grep -n -E '"/(api/)?(carteiras-funcionais|contracheques|ferias|recursos-humanos|designacoes|notificacoes/dispositivos|pagamentos)/\*\*"' -- "$security_config"; then
  report "endpoint autenticado voltou a lista publica"
fi

exit "$failed"

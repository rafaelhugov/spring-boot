#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

failed=0

report() {
  echo "S7 secret scan: $*"
  failed=1
}

while IFS= read -r file; do
  case "$file" in
    *.jks|*.p12|*.pfx|*.pem|*.p8|*.key|*.keystore|*key.properties|.env|*.env|*.env.*|*credentials*.json|*service-account*.json)
      report "arquivo sensivel versionado: $file"
      ;;
  esac
done < <(git ls-files restapi/mobile-service restapi/restapi-common .gitlab-ci.yml)

if git grep -I -n -E -e '-----BEGIN (RSA |EC |DSA |OPENSSH |PRIVATE )?KEY-----|private_key|client_email.*gserviceaccount|storePassword[[:space:]]*=[[:space:]]*["'\''][^"'\'']+["'\'']|keyPassword[[:space:]]*=[[:space:]]*["'\''][^"'\'']+["'\'']' -- restapi/mobile-service restapi/restapi-common ':!restapi/mobile-service/seguranca/**'; then
  report "padrao de segredo encontrado em arquivo versionado"
fi

exit "$failed"

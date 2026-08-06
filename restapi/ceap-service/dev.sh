#!/usr/bin/env bash
set -euo pipefail

# Executa o ncap-backend em modo desenvolvimento carregando variáveis do .env
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

if [[ -f .env ]]; then
  echo "[dev.sh] Carregando variáveis de .env..."
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
else
  echo "[dev.sh] Aviso: arquivo .env não encontrado; seguindo com variáveis atuais."
fi

PROFILE_VALUE="${SPRING_PROFILES_ACTIVE:-dev,insecure}"
echo "[dev.sh] Iniciando com SPRING_PROFILES_ACTIVE=$PROFILE_VALUE"

# Requer Maven instalado no PATH
mvn spring-boot:run

#!/usr/bin/env bash
set -euo pipefail

# Executa o eleicao-service em modo desenvolvimento carregando variáveis do .env
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

if [[ -f .env.dev ]]; then
  echo "[dev.sh] Carregando variáveis de .env.dev..."
  set -a
  source .env.dev
  set +a
else
  echo "[dev.sh] Aviso: arquivo .env.dev não encontrado; seguindo com variáveis atuais."
fi

PROFILE_VALUE="${SPRING_PROFILES_ACTIVE:-dev}"
echo "[dev.sh] Iniciando com SPRING_PROFILES_ACTIVE=$PROFILE_VALUE"

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=$PROFILE_VALUE"

#!/usr/bin/env bash
set -euo pipefail

# release.sh — Unified Backend (cursos-service) and Frontend (cursos-web)
# Uso:
#   ./release.sh 0.3.1 [--push]
#   ./release.sh patch|minor|major [--push]
#
# Efeito:
# - Atualiza versão no pom.xml e package.json
# - Gera o JAR (mvn clean package -DskipTests) e Build do Vue (npm run build)
# - Constrói imagens de ambos via scripts originais de docker
# - Opcionalmente faz docker push das tags

REGISTRY_DEFAULT="gavriel.mppb.mp.br:5000"
DO_PUSH=false
ORIGINAL_ARGS=("$@")

if [[ $# -lt 1 ]]; then
  echo "Uso: $0 <nova-versao|patch|minor|major> [--push]"
  exit 1
fi

NEWVER_OR_BUMP=$1
shift || true
if [[ ${1:-} == "--push" ]]; then
  DO_PUSH=true
  shift || true
fi

cd "$(dirname "$0")"
BACKEND_DIR=$(pwd)
FRONTEND_DIR=$(realpath "../../../mppb-vue/cursos-web")

ensure_docker_access() {
  if docker ps >/dev/null 2>&1; then
    return 0
  fi

  if [[ ${RELEASE_REEXEC_DOCKER:-} != "1" ]] &&
     getent group docker | grep -Eq "(^|,)$(id -un)(,|$)" &&
     ! id -nG | tr ' ' '\n' | grep -qx docker; then
    local cmd
    printf -v cmd '%q ' "$0" "${ORIGINAL_ARGS[@]}"
    if sg docker -c "docker ps >/dev/null 2>&1" >/dev/null 2>&1; then
      echo "[release] Sessao atual ainda nao carregou o grupo docker; reexecutando com 'sg docker'..."
      exec sg docker -c "RELEASE_REEXEC_DOCKER=1 $cmd"
    fi
  fi

  echo "[release] Sem permissao para acessar o Docker."
  exit 1
}

if ! docker ps >/dev/null 2>&1; then
  ensure_docker_access
fi

# --- BACKEND VERSION BUMP ---
echo "[release] Processando Backend..."
cd "$BACKEND_DIR"
CURRENT_VER=$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)

bump_semver() {
  local ver=$1 kind=$2
  IFS='.' read -r MA MI PA <<<"$ver"
  case "$kind" in
    major) MA=$((MA+1)); MI=0; PA=0 ;;
    minor) MI=$((MI+1)); PA=0 ;;
    patch) PA=$((PA+1)) ;;
    *) echo "Tipo de bump inválido: $kind"; return 1 ;;
  esac
  echo "$MA.$MI.$PA"
}

if [[ "$NEWVER_OR_BUMP" =~ ^(major|minor|patch)$ ]]; then
  NEW_VER=$(bump_semver "$CURRENT_VER" "$NEWVER_OR_BUMP")
else
  NEW_VER="$NEWVER_OR_BUMP"
fi

echo "[release] Backend: Versão atual $CURRENT_VER -> Nova: $NEW_VER"
mvn -q versions:set -DnewVersion="$NEW_VER"

# --- FRONTEND VERSION BUMP ---
echo "[release] Processando Frontend..."
cd "$FRONTEND_DIR"
FRONT_CURRENT_VER=$(node -p "require('./package.json').version")

if [[ "$NEWVER_OR_BUMP" =~ ^(major|minor|patch)$ ]]; then
  npm version "$NEWVER_OR_BUMP" --no-git-tag-version >/dev/null
else
  npm version "$NEWVER_OR_BUMP" --no-git-tag-version >/dev/null
fi

FRONT_NEW_VER=$(node -p "require('./package.json').version")
echo "[release] Frontend: Versão atual $FRONT_CURRENT_VER -> Nova: $FRONT_NEW_VER"

# --- BUILD BACKEND ---
echo "[release] Fazendo build do Backend..."
cd "$BACKEND_DIR"
mvn -q -Dmaven.test.skip=true clean package

# --- BUILD FRONTEND ---
echo "[release] Fazendo build do Frontend..."
cd "$FRONTEND_DIR"
npm run -s build

# --- DOCKER BUILD BACKEND ---
echo "[release] Construindo imagens do Backend..."
cd "$BACKEND_DIR"
./docker-build.sh

# --- DOCKER BUILD FRONTEND ---
echo "[release] Construindo imagens do Frontend..."
cd "$FRONTEND_DIR"
./docker.sh

# --- PUSH ---
if $DO_PUSH; then
  echo "[release] Fazendo docker push para Backend..."
  docker push "$REGISTRY_DEFAULT/cursos-api:$NEW_VER"
  docker push "$REGISTRY_DEFAULT/cursos-api:latest"

  echo "[release] Fazendo docker push para Frontend..."
  docker push "$REGISTRY_DEFAULT/cursos-web:$FRONT_NEW_VER"
  docker push "$REGISTRY_DEFAULT/cursos-web:latest"
fi

echo "[release] Concluído com sucesso (Backend: $NEW_VER, Frontend: $FRONT_NEW_VER)"

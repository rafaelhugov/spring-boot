#!/usr/bin/env bash
set -euo pipefail

# release.sh — Backend (cursos-service)
# Uso:
#   ./release.sh 0.3.1 [--push]
#   ./release.sh patch|minor|major [--push]
#
# Efeito:
# - Atualiza versão no pom.xml (mvn versions:set)
# - Gera o JAR (mvn clean package -DskipTests)
# - Constrói imagens via ./docker-build.sh (tags versão e latest)
# - Opcionalmente faz docker push das duas tags

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
  echo "[release] Grupos da sessao atual: $(id -nG)"
  echo "[release] Permissao atual do socket Docker:"
  ls -l /var/run/docker.sock
  echo
  if getent group docker | grep -Eq "(^|,)$(id -un)(,|$)" &&
     ! id -nG | tr ' ' '\n' | grep -qx docker; then
    echo "O usuario $(id -un) ja esta no grupo docker, mas esta sessao ainda nao carregou esse grupo."
    echo "Correcao definitiva: encerre a sessao do VDI/terminal e entre novamente."
    echo
  fi
  echo "Se o grupo docker acabou de ser adicionado, rode nesta mesma janela:"
  echo "  newgrp docker"
  echo
  echo "Se o socket estiver diferente de root:docker, corrija com:"
  echo "  sudo chown root:docker /var/run/docker.sock"
  echo "  sudo chmod 660 /var/run/docker.sock"
  echo
  echo "Depois teste com:"
  echo "  docker ps"
  exit 1
}

if ! docker ps >/dev/null 2>&1; then
  ensure_docker_access
fi

# Versão atual
CURRENT_VER=$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)

# Função de bump semver simples
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

echo "[release] Versão atual: $CURRENT_VER -> Nova: $NEW_VER"

# 1) Set versão no Maven
mvn -q versions:set -DnewVersion="$NEW_VER"

# 2) Build do JAR (pula também compilação de testes)
mvn -q -Dmaven.test.skip=true clean package

# 3) Build das imagens
./docker-build.sh

if $DO_PUSH; then
  echo "[release] Fazendo docker push para versão e latest..."
  docker push "$REGISTRY_DEFAULT/cursos-api:$NEW_VER"
  docker push "$REGISTRY_DEFAULT/cursos-api:latest"
fi

echo "[release] Concluído: $NEW_VER"

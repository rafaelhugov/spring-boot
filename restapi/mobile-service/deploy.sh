#!/bin/bash
set -e

# ─────────────────────────────────────────
# Cores e helpers
# ─────────────────────────────────────────
GREEN="\e[32m"; RED="\e[31m"; YELLOW="\e[33m"; CYAN="\e[36m"; BOLD="\e[1m"; RESET="\e[0m"
ok()    { echo -e "${GREEN}✅ $*${RESET}"; }
err()   { echo -e "${RED}❌ $*${RESET}"; exit 1; }
log()   { echo -e "\n${CYAN}${BOLD}──── $* ────${RESET}"; }
info()  { echo -e "   ${YELLOW}→ $*${RESET}"; }

REGISTRY="gavriel.mppb.mp.br:5000"
DIR_SCRIPT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BACKEND_DIR="$DIR_SCRIPT"
BACKEND_ROOT_DIR="$( cd "$DIR_SCRIPT/.." && pwd )"
FRONTEND_DIR="/home/rafaelhugo/projetosMPPB/mppb-vue/mobile-web"

# Configura PATH completo (Maven + Java 21 + Node via NVM)
if [ -d "$HOME/.sdkman/candidates/java/21.0.11-tem" ]; then
  export PATH="$HOME/.sdkman/candidates/java/21.0.11-tem/bin:$HOME/.sdkman/candidates/maven/current/bin:$HOME/.nvm/versions/node/v22.22.3/bin:$PATH"
else
  export PATH="$HOME/.sdkman/candidates/java/current/bin:$HOME/.sdkman/candidates/maven/current/bin:$HOME/.nvm/versions/node/v22.22.3/bin:$PATH"
fi
MAVEN="$HOME/.sdkman/candidates/maven/current/bin/mvn"
NPX="$HOME/.nvm/versions/node/v22.22.3/bin/npx"
NPM="$HOME/.nvm/versions/node/v22.22.3/bin/npm"

# ─────────────────────────────────────────
# Verificação antecipada do Docker
# ─────────────────────────────────────────
DOCKER_CMD=(docker)
if docker info > /dev/null 2>&1; then
  ok "Docker OK sem sudo"
elif sudo -n docker info > /dev/null 2>&1; then
  DOCKER_CMD=(sudo -n docker)
  ok "Docker OK com sudo sem senha"
else
  err "Docker sem permissão para este usuário. Execute 'sudo usermod -aG docker $USER' e faça logout/login, ou configure sudo sem senha para docker."
fi

# ─────────────────────────────────────────
# Lê versões atuais
# ─────────────────────────────────────────
CURRENT_BACKEND_VERSION=$(cd "$BACKEND_DIR" && $MAVEN help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
CURRENT_FRONTEND_VERSION=$(cd "$FRONTEND_DIR" && node -p "require('./package.json').version" 2>/dev/null || echo "0.0.1")

echo ""
echo -e "${BOLD}╔══════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║      DEPLOY — APLICATIVO MPPB        ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════╝${RESET}"
echo ""
echo -e "  Versão backend atual:  ${CYAN}${CURRENT_BACKEND_VERSION}${RESET}"
echo -e "  Versão frontend atual: ${CYAN}${CURRENT_FRONTEND_VERSION}${RESET}"
echo ""

# ─────────────────────────────────────────
# Seleção do tipo de bump
# ─────────────────────────────────────────
echo -e "${YELLOW}Selecione o tipo de versão:${RESET}"
echo "  [1] patch  — bug fix       (x.x.${BOLD}X${RESET})"
echo "  [2] minor  — nova feature  (x.${BOLD}X${RESET}.0)"
echo "  [3] major  — breaking      (${BOLD}X${RESET}.0.0)"
echo "  [4] manual — digitar versão"
echo ""
read -p "$(echo -e "${CYAN}Opção [1-4] (Enter = patch):${RESET} ")" BUMP_TYPE
BUMP_TYPE="${BUMP_TYPE:-1}"

# ─────────────────────────────────────────
# Calcula nova versão
# ─────────────────────────────────────────
bump_version() {
  local version=$1
  local type=$2
  IFS='.' read -r MAJOR MINOR PATCH <<< "$version"
  case "$type" in
    major) echo "$((MAJOR+1)).0.0" ;;
    minor) echo "${MAJOR}.$((MINOR+1)).0" ;;
    patch) echo "${MAJOR}.${MINOR}.$((PATCH+1))" ;;
    *) echo "$type" ;;  # versão manual
  esac
}

case "$BUMP_TYPE" in
  1) BUMP="patch" ;;
  2) BUMP="minor" ;;
  3) BUMP="major" ;;
  4)
    read -p "$(echo -e "${CYAN}Digite a nova versão (ex: 1.2.3):${RESET} ")" MANUAL_VERSION
    BUMP="$MANUAL_VERSION"
    ;;
  *) err "Opção inválida: $BUMP_TYPE" ;;
esac

NEW_VERSION=$(bump_version "$CURRENT_BACKEND_VERSION" "$BUMP")

echo ""
echo -e "${GREEN}${BOLD}Nova versão: ${NEW_VERSION}${RESET}"
echo ""
read -p "$(echo -e "${YELLOW}Confirmar deploy da versão ${NEW_VERSION}? [s/N]:${RESET} ")" CONFIRM
[[ "${CONFIRM,,}" == "s" ]] || err "Deploy cancelado pelo usuário."

# ─────────────────────────────────────────
# 1. Atualiza versões nos arquivos
# ─────────────────────────────────────────
log "1/6 — Atualizando versões"

info "pom.xml: ${CURRENT_BACKEND_VERSION} → ${NEW_VERSION}"
cd "$BACKEND_DIR"
$MAVEN versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false -q
ok "pom.xml atualizado"

info "package.json: ${CURRENT_FRONTEND_VERSION} → ${NEW_VERSION}"
cd "$FRONTEND_DIR"
$NPM version "$NEW_VERSION" --no-git-tag-version --allow-same-version -q 2>/dev/null || \
  node -e "
    const fs = require('fs');
    const pkg = JSON.parse(fs.readFileSync('package.json'));
    pkg.version = '$NEW_VERSION';
    fs.writeFileSync('package.json', JSON.stringify(pkg, null, 2) + '\n');
    console.log('package.json atualizado');
  "
ok "package.json atualizado"

# ─────────────────────────────────────────
# 2. Build do Backend (JAR)
# ─────────────────────────────────────────
log "2/6 — Build do Backend (Maven)"
cd "$BACKEND_ROOT_DIR"
$MAVEN install -N -Dmaven.test.skip=true -q
$MAVEN install -f restapi-common/pom.xml -Dmaven.test.skip=true -q
$MAVEN clean package -f mobile-service/pom.xml -Dmaven.test.skip=true -q
ok "JAR gerado: $(ls mobile-service/target/*.jar | grep -v original)"

# ─────────────────────────────────────────
# 3. Build + Push imagem Backend (Nome temporário: mobile-service-2)
# ─────────────────────────────────────────
log "3/6 — Docker Build + Push — Backend"
info "Tags: :${NEW_VERSION} e :latest"
cd "$BACKEND_DIR"
"${DOCKER_CMD[@]}" build --no-cache \
  -t $REGISTRY/mobile-service-2:${NEW_VERSION} \
  -t $REGISTRY/mobile-service-2:latest \
  . 2>&1 | grep -E "^(#|✓|DONE|ERROR|=>)" | tail -10
ok "Imagem backend gerada"

"${DOCKER_CMD[@]}" push $REGISTRY/mobile-service-2:${NEW_VERSION} 2>&1 | tail -3
"${DOCKER_CMD[@]}" push $REGISTRY/mobile-service-2:latest 2>&1 | tail -3
ok "Backend enviado: ${NEW_VERSION} + latest"

# ─────────────────────────────────────────
# 4. Build do Frontend (Quasar SPA)
# ─────────────────────────────────────────
log "4/6 — Build do Frontend (Quasar)"
cd "$FRONTEND_DIR"
if [ ! -d "node_modules" ]; then
  info "Instalando dependências npm..."
  $NPM install --silent
fi
$NPX quasar build 2>&1 | tail -5
ok "Frontend compilado em dist/spa"

# ─────────────────────────────────────────
# 5. Build + Push imagem Frontend (mobile-web)
# ─────────────────────────────────────────
log "5/6 — Docker Build + Push — Frontend"

info "Tags: :${NEW_VERSION} e :latest"
cd "$FRONTEND_DIR"
"${DOCKER_CMD[@]}" build --no-cache \
  -t $REGISTRY/mobile-web:${NEW_VERSION} \
  -t $REGISTRY/mobile-web:latest \
  -f Dockerfile.prod . 2>&1 | grep -E "^(#|✓|DONE|ERROR|=>)" | tail -10
ok "Imagem frontend gerada"

"${DOCKER_CMD[@]}" push $REGISTRY/mobile-web:${NEW_VERSION} 2>&1 | tail -3
"${DOCKER_CMD[@]}" push $REGISTRY/mobile-web:latest 2>&1 | tail -3
ok "Frontend enviado: ${NEW_VERSION} + latest"

# ─────────────────────────────────────────
# 6. Resumo final
# ─────────────────────────────────────────
log "6/6 — Deploy concluído"
echo ""
echo -e "${GREEN}${BOLD}═════════════════════════════════════════${RESET}"
echo -e "${GREEN}${BOLD}  ✅ VERSÃO ${NEW_VERSION} PUBLICADA COM SUCESSO! ${RESET}"
echo -e "${GREEN}${BOLD}═════════════════════════════════════════${RESET}"
echo ""
echo -e "  ${CYAN}Backend:  ${RESET} $REGISTRY/mobile-service-2:${NEW_VERSION}"
echo -e "  ${CYAN}Frontend: ${RESET} $REGISTRY/mobile-web:${NEW_VERSION}"
echo ""
echo -e "${YELLOW}${BOLD}Para ativar no Portainer:${RESET}"
echo "  1. Stacks → aplicativo-mppb (ou stack correspondente)"
echo "  2. ✅ Marque 'Re-pull image'"
echo "  3. Clique em 'Update the stack'"
echo ""

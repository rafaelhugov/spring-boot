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
BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$(realpath "$BACKEND_DIR/../../../mppb-vue/cursos-web")"

# Configura PATH completo (Maven + Java + Node via NVM) se necessário
export PATH="$HOME/.sdkman/candidates/java/current/bin:$HOME/.sdkman/candidates/maven/current/bin:$HOME/.nvm/versions/node/v22.22.3/bin:$PATH"
MAVEN="$HOME/.sdkman/candidates/maven/current/bin/mvn"
NPM="$HOME/.nvm/versions/node/v22.22.3/bin/npm"

# Fallback se não encontrar o sdkman/nvm local
if ! command -v mvn &> /dev/null; then MAVEN="mvn"; fi
if ! command -v npm &> /dev/null; then NPM="npm"; fi

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
CURRENT_BACKEND_VERSION=$(cd "$BACKEND_DIR" && $MAVEN help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "0.0.1")
CURRENT_FRONTEND_VERSION=$(cd "$FRONTEND_DIR" && node -p "require('./package.json').version" 2>/dev/null || echo "0.0.1")

echo ""
echo -e "${BOLD}╔══════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║       DEPLOY — CURSOS MPPB           ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════╝${RESET}"
echo ""
echo -e "  Versão backend atual:  ${CYAN}${CURRENT_BACKEND_VERSION}${RESET}"
echo -e "  Versão frontend atual: ${CYAN}${CURRENT_FRONTEND_VERSION}${RESET}"
echo ""

# ─────────────────────────────────────────
# Seleção do tipo de bump
# ─────────────────────────────────────────
echo -e "${YELLOW}Selecione o tipo de versão para Backend e Frontend:${RESET}"
echo "  [1] patch  — bug fix       (x.x.${BOLD}X${RESET})"
echo "  [2] minor  — nova feature  (x.${BOLD}X${RESET}.0)"
echo "  [3] major  — breaking      (${BOLD}X${RESET}.0.0)"
echo "  [4] manual — digitar versão (ambos terão a mesma)"
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

NEW_BACKEND_VERSION=$(bump_version "$CURRENT_BACKEND_VERSION" "$BUMP")
NEW_FRONTEND_VERSION=$(bump_version "$CURRENT_FRONTEND_VERSION" "$BUMP")

echo ""
echo -e "${GREEN}${BOLD}Nova versão Backend:  ${NEW_BACKEND_VERSION}${RESET}"
echo -e "${GREEN}${BOLD}Nova versão Frontend: ${NEW_FRONTEND_VERSION}${RESET}"
echo ""
read -p "$(echo -e "${YELLOW}Confirmar deploy das novas versões? [s/N]:${RESET} ")" CONFIRM
[[ "${CONFIRM,,}" == "s" ]] || err "Deploy cancelado pelo usuário."

# ─────────────────────────────────────────
# 1. Atualiza versões nos arquivos
# ─────────────────────────────────────────
log "1/6 — Atualizando versões"

info "pom.xml: ${CURRENT_BACKEND_VERSION} → ${NEW_BACKEND_VERSION}"
cd "$BACKEND_DIR"
$MAVEN versions:set -DnewVersion="$NEW_BACKEND_VERSION" -DgenerateBackupPoms=false -q
ok "pom.xml atualizado"

info "package.json: ${CURRENT_FRONTEND_VERSION} → ${NEW_FRONTEND_VERSION}"
cd "$FRONTEND_DIR"
$NPM version "$NEW_FRONTEND_VERSION" --no-git-tag-version --allow-same-version -q 2>/dev/null || \
  node -e "
    const fs = require('fs');
    const pkg = JSON.parse(fs.readFileSync('package.json'));
    pkg.version = '$NEW_FRONTEND_VERSION';
    fs.writeFileSync('package.json', JSON.stringify(pkg, null, 2) + '\n');
    console.log('package.json atualizado');
  "
ok "package.json atualizado"

# ─────────────────────────────────────────
# 2. Build do Backend (JAR)
# ─────────────────────────────────────────
log "2/6 — Build do Backend (Maven)"
cd "$BACKEND_DIR"
$MAVEN clean package -DskipTests -q
ok "JAR gerado: $(ls target/*.jar | grep -v original | head -1)"

# ─────────────────────────────────────────
# 3. Docker Build + Push — Backend
# ─────────────────────────────────────────
log "3/6 — Docker Build + Push — Backend"
cd "$BACKEND_DIR"
info "Tags: :${NEW_BACKEND_VERSION} e :latest"
if [ -f "./docker-build.sh" ]; then
  ./docker-build.sh >/dev/null
else
  "${DOCKER_CMD[@]}" build --no-cache -t $REGISTRY/cursos-api:${NEW_BACKEND_VERSION} -t $REGISTRY/cursos-api:latest . 2>&1 | grep -E "^(#|✓|DONE|ERROR|=>)" | tail -10
fi
ok "Imagem backend gerada"

"${DOCKER_CMD[@]}" push $REGISTRY/cursos-api:${NEW_BACKEND_VERSION} 2>&1 | tail -3
"${DOCKER_CMD[@]}" push $REGISTRY/cursos-api:latest 2>&1 | tail -3
ok "Backend enviado: ${NEW_BACKEND_VERSION} + latest"

# ─────────────────────────────────────────
# 4. Build do Frontend
# ─────────────────────────────────────────
log "4/6 — Build do Frontend"
cd "$FRONTEND_DIR"
if [ ! -d "node_modules" ]; then
  info "Instalando dependências npm..."
  $NPM install --silent
fi
$NPM run build -s 2>&1 | tail -5
ok "Frontend compilado"

# ─────────────────────────────────────────
# 5. Docker Build + Push — Frontend
# ─────────────────────────────────────────
log "5/6 — Docker Build + Push — Frontend"
cd "$FRONTEND_DIR"
info "Tags: :${NEW_FRONTEND_VERSION} e :latest"

if [ -f "./docker.sh" ]; then
  ./docker.sh >/dev/null
else
  "${DOCKER_CMD[@]}" build --no-cache -t $REGISTRY/cursos-web:${NEW_FRONTEND_VERSION} -t $REGISTRY/cursos-web:latest . 2>&1 | grep -E "^(#|✓|DONE|ERROR|=>)" | tail -10
fi
ok "Imagem frontend gerada"

"${DOCKER_CMD[@]}" push $REGISTRY/cursos-web:${NEW_FRONTEND_VERSION} 2>&1 | tail -3
"${DOCKER_CMD[@]}" push $REGISTRY/cursos-web:latest 2>&1 | tail -3
ok "Frontend enviado: ${NEW_FRONTEND_VERSION} + latest"

# ─────────────────────────────────────────
# 6. Resumo final
# ─────────────────────────────────────────
log "6/6 — Deploy concluído"
echo ""
echo -e "${GREEN}${BOLD}════════════════════════════════════════${RESET}"
echo -e "${GREEN}${BOLD}  ✅ VERSÕES PUBLICADAS COM SUCESSO!${RESET}"
echo -e "${GREEN}${BOLD}════════════════════════════════════════${RESET}"
echo ""
echo -e "  ${CYAN}Backend: ${RESET} $REGISTRY/cursos-api:${NEW_BACKEND_VERSION}"
echo -e "  ${CYAN}Frontend:${RESET} $REGISTRY/cursos-web:${NEW_FRONTEND_VERSION}"
echo ""
echo -e "${YELLOW}${BOLD}Para ativar no Portainer:${RESET}"
echo "  1. Stacks → cursos-mppb"
echo "  2. ✅ Marque 'Re-pull image'"
echo "  3. Clique em 'Update the stack'"
echo ""
echo -e "${YELLOW}Dica: Você pode fixar uma versão específica no stack:${RESET}"
echo "  image: $REGISTRY/cursos-api:${NEW_BACKEND_VERSION}"
echo "  image: $REGISTRY/cursos-web:${NEW_FRONTEND_VERSION}"
echo ""

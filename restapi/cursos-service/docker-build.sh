#!/bin/bash

COLOR_RED="31"
COLOR_GREEN="32"

print_error() {
  echo -e "\e[${COLOR_RED}mErro:\e[0m $1"
}

print_ok() {
  echo -e "\e[${COLOR_GREEN}m$1\e[0m"
}

# Lê a versão do pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if [ "" == "$VERSION" ]; then
  print_error "Não foi possível determinar a versão do pom.xml"
  exit 1
fi

print_ok ">>> Compilando versão $VERSION com Maven (sem testes)..."
mvn package -DskipTests -q #isso não tinha

if [ $? -ne 0 ]; then
  print_error "Falha na compilação Maven. Corrija os erros antes de continuar."
  exit 1
fi

print_ok ">>> Build Maven concluído. Preparando arquivos de motor de PDF..."

# Garante que os diretórios existam
mkdir -p scripts public

# Copia os arquivos do frontend para o contexto do docker
cp ../../../mppb-vue/cursos-web/scripts/pdf-engine.js scripts/
cp ../../../mppb-vue/cursos-web/public/pdf-template-*.html public/

print_ok ">>> Gerando imagem Docker..."

docker build -f Dockerfile.prod \
  -t gavriel.mppb.mp.br:5000/cursos-api:$VERSION \
  -t gavriel.mppb.mp.br:5000/cursos-api:latest \
  -t localhost:5000/cursos-api:$VERSION \
  -t localhost:5000/cursos-api:latest \
  .

if [ $? -eq 0 ]; then
  print_ok ">>> Imagem cursos-api:$VERSION gerada com sucesso!"
else
  print_error "Falha na geração da imagem Docker."
  exit 1
fi

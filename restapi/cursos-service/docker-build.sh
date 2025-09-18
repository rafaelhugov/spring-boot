#!/bin/bash

COLOR_RED="31"
COLOR_GREEN="32"

#VERSION=$1
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

print_error() {
  echo -e "\e[${COLOR_RED}mErro:\e[0m $1"
}

if [ "" == "$VERSION" ]; then
  print_error "Necessário informar a versão"
  exit
fi


docker build \
  -t gavriel.mppb.mp.br:5000/cursos-api:$VERSION \
  -t gavriel.mppb.mp.br:5000/cursos-api:latest \
  -t localhost:5000/cursos-api:$VERSION \
  -t localhost:5000/cursos-api:latest \
  .

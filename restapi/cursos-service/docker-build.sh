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

if [ "" == "$DOCKER_IO_URL" ]; then
  print_error "Necessário definir variável de ambiente DOCKER_IO_URL."
  exit 1
fi

docker build \
  -t $DOCKER_IO_URL/cobranca-api:$VERSION \
  -t $DOCKER_IO_URL/cobranca-api:latest \
  -t localhost:5000/cobranca-api:$VERSION \
  -t localhost:5000/cobranca-api:latest \
  .

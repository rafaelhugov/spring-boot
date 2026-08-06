#!/bin/bash
# Script de limpeza: remove o pacote boilerplate legado do ncap
# Execute uma única vez após confirmar que o novo pacote br.gov.mppb.ncap está correto.

OLD_PACKAGE_DIR="src/main/java/br/gov/mppb/boilerplate"

if [ -d "$OLD_PACKAGE_DIR" ]; then
    echo "Removendo diretório legado: $OLD_PACKAGE_DIR"
    rm -rf "$OLD_PACKAGE_DIR"
    echo "✅ Concluído. Diretório boilerplate removido com sucesso."
else
    echo "⚠️  Diretório não encontrado (já removido?): $OLD_PACKAGE_DIR"
fi

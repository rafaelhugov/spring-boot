package br.gov.mppb.athena.dominio;

final class TextoObrigatorio {

    private TextoObrigatorio() {
    }

    static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.strip();
    }
}

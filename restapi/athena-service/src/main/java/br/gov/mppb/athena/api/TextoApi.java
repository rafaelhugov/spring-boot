package br.gov.mppb.athena.api;

final class TextoApi {

    private TextoApi() {
    }

    static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.strip();
    }
}

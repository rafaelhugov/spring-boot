package br.gov.mppb.athena.aplicacao;

final class TextoAplicacao {

    private TextoAplicacao() {
    }

    static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.strip();
    }
}

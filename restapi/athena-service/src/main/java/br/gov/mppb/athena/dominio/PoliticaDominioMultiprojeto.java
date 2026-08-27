package br.gov.mppb.athena.dominio;

import java.util.Locale;

public final class PoliticaDominioMultiprojeto {

    private static final String TERMO_PROJETO_PILOTO = "sisplan";

    private PoliticaDominioMultiprojeto() {
    }

    public static void rejeitarTermoDeProjetoPiloto(String valor, String campo) {
        if (valor != null && valor.toLowerCase(Locale.ROOT).contains(TERMO_PROJETO_PILOTO)) {
            throw new IllegalArgumentException(campo + " nao pode incorporar terminologia de projeto piloto.");
        }
    }
}

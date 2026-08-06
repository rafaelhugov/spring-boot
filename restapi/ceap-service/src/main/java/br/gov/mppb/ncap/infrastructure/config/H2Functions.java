package br.gov.mppb.ncap.infrastructure.config;

import java.text.Normalizer;

public class H2Functions {
    public static String funcaoSemAcentos(String valor) {
        if (valor == null) {
            return null;
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}

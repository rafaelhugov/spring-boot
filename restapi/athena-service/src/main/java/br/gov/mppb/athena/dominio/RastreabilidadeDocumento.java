package br.gov.mppb.athena.dominio;

import java.util.Set;

public record RastreabilidadeDocumento(Set<String> fontes) {

    public RastreabilidadeDocumento {
        if (fontes == null || fontes.isEmpty()) {
            throw new IllegalArgumentException("fontes de rastreabilidade devem ser informadas.");
        }
        fontes = Set.copyOf(fontes);
    }
}

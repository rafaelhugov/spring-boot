package br.gov.mppb.athena.arquitetura;

import java.util.Set;

public record ResponsabilidadeCamada(
        CamadaAthena camada,
        String pacote,
        Set<String> responsabilidades
) {
    public ResponsabilidadeCamada {
        responsabilidades = Set.copyOf(responsabilidades);
    }
}

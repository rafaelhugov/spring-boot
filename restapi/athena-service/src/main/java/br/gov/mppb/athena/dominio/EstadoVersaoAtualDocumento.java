package br.gov.mppb.athena.dominio;

import java.util.UUID;

public record EstadoVersaoAtualDocumento(
        UUID idDocumento,
        UUID idVersaoAtual,
        long versaoControle
) {

    public EstadoVersaoAtualDocumento {
        if (idDocumento == null) {
            throw new IllegalArgumentException("documento deve ser informado.");
        }
        if (versaoControle < 0) {
            throw new IllegalArgumentException("versao de controle nao pode ser negativa.");
        }
    }
}

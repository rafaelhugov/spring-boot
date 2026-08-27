package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ComentarioRegistrado(
        ReferenciaAvaliacaoDocumental referencia,
        IdentificadorUsuario autor,
        OffsetDateTime data,
        String texto,
        DecisaoAvaliacao decisaoRelacionada
) {
}

package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record AvaliacaoRegistrada(
        ReferenciaAvaliacaoDocumental referencia,
        IdentificadorUsuario responsavel,
        OffsetDateTime data,
        DecisaoAvaliacao decisao,
        String justificativa
) {
}

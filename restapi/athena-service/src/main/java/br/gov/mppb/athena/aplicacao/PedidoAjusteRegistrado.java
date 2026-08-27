package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record PedidoAjusteRegistrado(
        ReferenciaAvaliacaoDocumental documentoOriginal,
        IdentificadorUsuario solicitante,
        OffsetDateTime data,
        String justificativa,
        String processoExternoResponsavel
) {
}

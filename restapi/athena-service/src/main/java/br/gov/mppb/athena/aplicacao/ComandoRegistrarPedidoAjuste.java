package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ComandoRegistrarPedidoAjuste(
        ReferenciaAvaliacaoDocumental referencia,
        IdentificadorUsuario solicitante,
        OffsetDateTime data,
        String justificativa
) {

    public ComandoRegistrarPedidoAjuste {
        if (referencia == null) {
            throw new IllegalArgumentException("referencia do pedido de ajuste deve ser informada.");
        }
        if (solicitante == null) {
            throw new IllegalArgumentException("solicitante do pedido de ajuste deve ser informado.");
        }
        if (data == null) {
            throw new IllegalArgumentException("data do pedido de ajuste deve ser informada.");
        }
        justificativa = TextoAplicacao.validar(justificativa, "justificativa do pedido de ajuste");
    }
}

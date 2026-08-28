package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ComandoTratarPedidoAjuste(
        IdentificadorUsuario responsavelTecnico,
        OffsetDateTime dataTratamento,
        EstadoSolicitacaoAjuste decisao,
        String justificativa
) {

    public ComandoTratarPedidoAjuste {
        if (responsavelTecnico == null) {
            throw new IllegalArgumentException("responsavel tecnico pelo tratamento deve ser informado.");
        }
        if (dataTratamento == null) {
            throw new IllegalArgumentException("data do tratamento deve ser informada.");
        }
        if (decisao == null || decisao == EstadoSolicitacaoAjuste.PENDENTE) {
            throw new IllegalArgumentException("decisao de tratamento deve acolher ou nao acolher a solicitacao.");
        }
        if (decisao == EstadoSolicitacaoAjuste.NAO_ACOLHIDA) {
            justificativa = TextoAplicacao.validar(justificativa, "justificativa do nao acolhimento");
        } else if (justificativa != null && justificativa.isBlank()) {
            justificativa = null;
        }
    }
}

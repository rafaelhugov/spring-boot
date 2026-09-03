package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record PedidoAjusteRegistrado(
        ReferenciaAvaliacaoDocumental documentoOriginal,
        IdentificadorUsuario solicitante,
        OffsetDateTime data,
        String justificativa,
        IdentificadorUsuario responsavelTecnico,
        EstadoSolicitacaoAjuste estado,
        OffsetDateTime dataTratamento,
        String justificativaTratamento,
        boolean permiteGerarNovaVersao
) {

    public PedidoAjusteRegistrado {
        if (documentoOriginal == null) {
            throw new IllegalArgumentException("documento original da solicitacao deve ser informado.");
        }
        if (solicitante == null) {
            throw new IllegalArgumentException("solicitante da solicitacao deve ser informado.");
        }
        if (data == null) {
            throw new IllegalArgumentException("data da solicitacao deve ser informada.");
        }
        justificativa = TextoAplicacao.validar(justificativa, "justificativa da solicitacao");
        if (responsavelTecnico == null) {
            throw new IllegalArgumentException("responsavel tecnico da solicitacao deve ser informado.");
        }
        if (estado == null) {
            throw new IllegalArgumentException("estado da solicitacao deve ser informado.");
        }
        if (estado == EstadoSolicitacaoAjuste.NAO_ACOLHIDA) {
            justificativaTratamento = TextoAplicacao.validar(
                    justificativaTratamento,
                    "justificativa do nao acolhimento"
            );
        }
        if (estado == EstadoSolicitacaoAjuste.PENDENTE) {
            dataTratamento = null;
            justificativaTratamento = null;
            permiteGerarNovaVersao = false;
        }
        if (estado == EstadoSolicitacaoAjuste.ACOLHIDA) {
            permiteGerarNovaVersao = true;
        }
        if (estado != EstadoSolicitacaoAjuste.PENDENTE && dataTratamento == null) {
            throw new IllegalArgumentException("data do tratamento deve ser informada.");
        }
    }

    public static PedidoAjusteRegistrado pendente(
            ReferenciaAvaliacaoDocumental documentoOriginal,
            IdentificadorUsuario solicitante,
            OffsetDateTime data,
            String justificativa,
            IdentificadorUsuario responsavelTecnico
    ) {
        return new PedidoAjusteRegistrado(
                documentoOriginal,
                solicitante,
                data,
                justificativa,
                responsavelTecnico,
                EstadoSolicitacaoAjuste.PENDENTE,
                null,
                null,
                false
        );
    }

    public PedidoAjusteRegistrado tratar(ComandoTratarPedidoAjuste comando) {
        if (estado != EstadoSolicitacaoAjuste.PENDENTE) {
            throw new IllegalStateException("solicitacao de ajuste ja possui tratamento registrado.");
        }
        if (!responsavelTecnico.equals(comando.responsavelTecnico())) {
            throw new IllegalArgumentException("tratamento deve ser realizado pelo responsavel tecnico da solicitacao.");
        }
        return new PedidoAjusteRegistrado(
                documentoOriginal,
                solicitante,
                data,
                justificativa,
                responsavelTecnico,
                comando.decisao(),
                comando.dataTratamento(),
                comando.justificativa(),
                comando.decisao() == EstadoSolicitacaoAjuste.ACOLHIDA
        );
    }
}

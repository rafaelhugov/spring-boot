package br.gov.mppb.athena.aplicacao;

import java.util.List;

public record HistoricoAvaliacaoConfirmacao(
        List<AvaliacaoRegistrada> avaliacoes,
        List<ComentarioRegistrado> comentarios,
        List<PedidoAjusteRegistrado> pedidosAjuste,
        List<ConfirmacaoEntendimentoRegistrada> confirmacoes
) {

    public HistoricoAvaliacaoConfirmacao {
        avaliacoes = List.copyOf(avaliacoes);
        comentarios = List.copyOf(comentarios);
        pedidosAjuste = List.copyOf(pedidosAjuste);
        confirmacoes = List.copyOf(confirmacoes);
    }
}

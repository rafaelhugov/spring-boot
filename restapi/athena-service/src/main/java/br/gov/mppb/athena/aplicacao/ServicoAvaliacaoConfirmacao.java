package br.gov.mppb.athena.aplicacao;

import java.util.ArrayList;
import java.util.List;

public class ServicoAvaliacaoConfirmacao {

    private final List<AvaliacaoRegistrada> avaliacoes = new ArrayList<>();
    private final List<ComentarioRegistrado> comentarios = new ArrayList<>();
    private final List<PedidoAjusteRegistrado> pedidosAjuste = new ArrayList<>();
    private final List<ConfirmacaoEntendimentoRegistrada> confirmacoes = new ArrayList<>();

    public synchronized AvaliacaoRegistrada registrarAvaliacao(ComandoRegistrarAvaliacao comando) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de avaliacao deve ser informado.");
        }
        if (!comando.avaliacaoHabilitadaPorPolitica()) {
            throw new IllegalStateException("avaliacao nao habilitada por politica institucional aplicavel.");
        }

        AvaliacaoRegistrada avaliacao = new AvaliacaoRegistrada(
                comando.referencia(),
                comando.responsavel(),
                comando.data(),
                comando.decisao(),
                comando.justificativa()
        );
        avaliacoes.add(avaliacao);
        return avaliacao;
    }

    public synchronized ComentarioRegistrado registrarComentario(ComandoRegistrarComentario comando) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de comentario deve ser informado.");
        }

        ComentarioRegistrado comentario = new ComentarioRegistrado(
                comando.referencia(),
                comando.autor(),
                comando.data(),
                comando.texto(),
                comando.decisaoRelacionada()
        );
        comentarios.add(comentario);
        return comentario;
    }

    public synchronized PedidoAjusteRegistrado registrarPedidoAjuste(ComandoRegistrarPedidoAjuste comando) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de pedido de ajuste deve ser informado.");
        }

        PedidoAjusteRegistrado pedido = new PedidoAjusteRegistrado(
                comando.referencia(),
                comando.solicitante(),
                comando.data(),
                comando.justificativa(),
                comando.processoExternoResponsavel()
        );
        pedidosAjuste.add(pedido);
        return pedido;
    }

    public synchronized ConfirmacaoEntendimentoRegistrada confirmarEntendimento(ComandoConfirmarEntendimento comando) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de confirmacao deve ser informado.");
        }

        ConfirmacaoEntendimentoRegistrada confirmacao = new ConfirmacaoEntendimentoRegistrada(
                comando.projeto(),
                comando.documento(),
                comando.versaoConfirmada(),
                comando.responsavel(),
                comando.data(),
                comando.escopo()
        );
        confirmacoes.add(confirmacao);
        return confirmacao;
    }

    public synchronized HistoricoAvaliacaoConfirmacao historico() {
        return new HistoricoAvaliacaoConfirmacao(avaliacoes, comentarios, pedidosAjuste, confirmacoes);
    }
}

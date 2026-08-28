package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import br.gov.mppb.athena.seguranca.ResultadoAutorizacao;
import java.util.ArrayList;
import java.util.List;

public class ServicoAvaliacaoConfirmacao {

    private final List<AvaliacaoRegistrada> avaliacoes = new ArrayList<>();
    private final List<ComentarioRegistrado> comentarios = new ArrayList<>();
    private final List<PedidoAjusteRegistrado> pedidosAjuste = new ArrayList<>();
    private final List<ConfirmacaoEntendimentoRegistrada> confirmacoes = new ArrayList<>();

    public synchronized AvaliacaoRegistrada registrarAvaliacao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            ComandoRegistrarAvaliacao comando
    ) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de avaliacao deve ser informado.");
        }
        autorizar(projeto, contexto, comando.referencia().projeto(), OperacaoProjeto.AVALIAR);
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

    public synchronized ComentarioRegistrado registrarComentario(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            ComandoRegistrarComentario comando
    ) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de comentario deve ser informado.");
        }
        autorizar(projeto, contexto, comando.referencia().projeto(), OperacaoProjeto.COMENTAR);

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

    public synchronized PedidoAjusteRegistrado registrarPedidoAjuste(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            ComandoRegistrarPedidoAjuste comando
    ) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de pedido de ajuste deve ser informado.");
        }
        autorizar(projeto, contexto, comando.referencia().projeto(), OperacaoProjeto.AVALIAR);

        IdentificadorUsuario responsavelTecnico = responsavelTecnico(projeto);
        PedidoAjusteRegistrado pedido = PedidoAjusteRegistrado.pendente(
                comando.referencia(),
                comando.solicitante(),
                comando.data(),
                comando.justificativa(),
                responsavelTecnico
        );
        pedidosAjuste.add(pedido);
        return pedido;
    }

    public synchronized PedidoAjusteRegistrado tratarPedidoAjuste(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            PedidoAjusteRegistrado pedido,
            ComandoTratarPedidoAjuste comando
    ) {
        if (pedido == null) {
            throw new IllegalArgumentException("pedido de ajuste deve ser informado.");
        }
        if (comando == null) {
            throw new IllegalArgumentException("comando de tratamento deve ser informado.");
        }
        autorizar(projeto, contexto, pedido.documentoOriginal().projeto(), OperacaoProjeto.AVALIAR);
        if (!responsavelTecnico(projeto).equals(comando.responsavelTecnico())) {
            throw new IllegalArgumentException("tratamento deve ser realizado pelo responsavel tecnico do projeto.");
        }

        int indice = pedidosAjuste.indexOf(pedido);
        if (indice < 0) {
            throw new IllegalArgumentException("pedido de ajuste deve estar preservado no historico antes do tratamento.");
        }
        PedidoAjusteRegistrado tratado = pedido.tratar(comando);
        pedidosAjuste.set(indice, tratado);
        return tratado;
    }

    public synchronized ConfirmacaoEntendimentoRegistrada confirmarEntendimento(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            ComandoConfirmarEntendimento comando
    ) {
        if (comando == null) {
            throw new IllegalArgumentException("comando de confirmacao deve ser informado.");
        }
        autorizar(projeto, contexto, comando.projeto(), OperacaoProjeto.CONFIRMAR);

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

    private static void autorizar(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            IdentificadorProjeto projetoDoComando,
            OperacaoProjeto operacao
    ) {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto protegido deve ser informado.");
        }
        if (!projeto.identificador().equals(projetoDoComando)) {
            throw new IllegalArgumentException("operacao deve referenciar o projeto protegido informado.");
        }

        ResultadoAutorizacao autorizacao = PoliticaSegurancaAthena.autorizar(projeto, contexto, operacao);
        if (!autorizacao.permitido()) {
            throw new IllegalStateException("operacao protegida nao autorizada: " + autorizacao.motivo());
        }
    }

    private static IdentificadorUsuario responsavelTecnico(ProjetoInstitucional projeto) {
        return projeto.participantes().stream()
                .filter(participante -> participante.papel() == PapelProjeto.RESPONSAVEL_TECNICO)
                .map(participante -> participante.usuario().identificador())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("projeto deve possuir responsavel tecnico para tratar solicitacao de ajuste."));
    }
}

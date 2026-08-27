package br.gov.mppb.athena.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class ServicoAvaliacaoConfirmacaoTest {

    private final ServicoAvaliacaoConfirmacao servico = new ServicoAvaliacaoConfirmacao();
    private final IdentificadorUsuario responsavel = new IdentificadorUsuario("usuario-avaliador");
    private final ReferenciaAvaliacaoDocumental referencia = new ReferenciaAvaliacaoDocumental(
            new IdentificadorProjeto("ATHENA"),
            "DOC-ENTENDIMENTO",
            2,
            "ESP.REQ.006"
    );

    @Test
    void deveRegistrarFluxosComEntradasSaidasTransicoesEHistoricoObrigatorio() {
        AvaliacaoRegistrada avaliacao = servico.registrarAvaliacao(new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Item consistente com a linha de base.",
                true
        ));
        ComentarioRegistrado comentario = servico.registrarComentario(new ComandoRegistrarComentario(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:05:00-03:00"),
                "Comentario associado ao item avaliado.",
                DecisaoAvaliacao.APROVADO
        ));
        PedidoAjusteRegistrado pedido = servico.registrarPedidoAjuste(new ComandoRegistrarPedidoAjuste(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:10:00-03:00"),
                "Inconsistencia localizada em campo recebido.",
                "processo-documental-externo"
        ));
        ConfirmacaoEntendimentoRegistrada confirmacao = servico.confirmarEntendimento(new ComandoConfirmarEntendimento(
                referencia.projeto(),
                referencia.documento(),
                referencia.versao(),
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:15:00-03:00"),
                "Necessidade, contexto, problema e objetivos."
        ));

        HistoricoAvaliacaoConfirmacao historico = servico.historico();

        assertThat(historico.avaliacoes()).containsExactly(avaliacao);
        assertThat(historico.comentarios()).containsExactly(comentario);
        assertThat(historico.pedidosAjuste()).containsExactly(pedido);
        assertThat(historico.confirmacoes()).containsExactly(confirmacao);
        assertThat(avaliacao)
                .extracting(AvaliacaoRegistrada::referencia, AvaliacaoRegistrada::responsavel,
                        AvaliacaoRegistrada::data, AvaliacaoRegistrada::decisao,
                        AvaliacaoRegistrada::justificativa)
                .containsExactly(referencia, responsavel, OffsetDateTime.parse("2026-08-27T09:00:00-03:00"),
                        DecisaoAvaliacao.APROVADO, "Item consistente com a linha de base.");
        assertThat(comentario)
                .extracting(ComentarioRegistrado::referencia, ComentarioRegistrado::autor,
                        ComentarioRegistrado::data, ComentarioRegistrado::decisaoRelacionada)
                .containsExactly(referencia, responsavel, OffsetDateTime.parse("2026-08-27T09:05:00-03:00"),
                        DecisaoAvaliacao.APROVADO);
    }

    @Test
    void deveManterDecisoesIndividuaisConsultaveisAposConfirmacaoFormal() {
        AvaliacaoRegistrada primeiraAvaliacao = servico.registrarAvaliacao(new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T10:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Primeira decisao individual.",
                true
        ));
        AvaliacaoRegistrada segundaAvaliacao = servico.registrarAvaliacao(new ComandoRegistrarAvaliacao(
                new ReferenciaAvaliacaoDocumental(referencia.projeto(), referencia.documento(), 2, "ESP.REQ.007"),
                responsavel,
                OffsetDateTime.parse("2026-08-27T10:10:00-03:00"),
                DecisaoAvaliacao.AJUSTE_SOLICITADO,
                "Segunda decisao individual.",
                true
        ));

        servico.confirmarEntendimento(new ComandoConfirmarEntendimento(
                referencia.projeto(),
                referencia.documento(),
                2,
                responsavel,
                OffsetDateTime.parse("2026-08-27T10:20:00-03:00"),
                "Escopo consolidado do documento."
        ));

        assertThat(servico.historico().avaliacoes())
                .containsExactly(primeiraAvaliacao, segundaAvaliacao)
                .extracting(AvaliacaoRegistrada::decisao)
                .containsExactly(DecisaoAvaliacao.APROVADO, DecisaoAvaliacao.AJUSTE_SOLICITADO);
    }

    @Test
    void devePreservarDocumentoOriginalAoRegistrarPedidoDeAjuste() {
        PedidoAjusteRegistrado pedido = servico.registrarPedidoAjuste(new ComandoRegistrarPedidoAjuste(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T11:00:00-03:00"),
                "Campo recebido diverge da fonte aprovada.",
                "processo-documental-externo"
        ));

        assertThat(pedido.documentoOriginal()).isEqualTo(referencia);
        assertThat(pedido.documentoOriginal().documento()).isEqualTo("DOC-ENTENDIMENTO");
        assertThat(pedido.documentoOriginal().versao()).isEqualTo(2);
        assertThat(pedido.processoExternoResponsavel()).isEqualTo("processo-documental-externo");
        assertThat(servico.historico().pedidosAjuste()).containsExactly(pedido);
    }

    @Test
    void deveRegistrarConfirmacaoSemSubstituirAvaliacoesIndividuais() {
        AvaliacaoRegistrada avaliacao = servico.registrarAvaliacao(new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T12:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Avaliacao individual preservada.",
                true
        ));

        ConfirmacaoEntendimentoRegistrada confirmacao = servico.confirmarEntendimento(new ComandoConfirmarEntendimento(
                referencia.projeto(),
                referencia.documento(),
                2,
                responsavel,
                OffsetDateTime.parse("2026-08-27T12:30:00-03:00"),
                "Confirmacao do entendimento apresentado."
        ));

        assertThat(confirmacao)
                .extracting(ConfirmacaoEntendimentoRegistrada::responsavel,
                        ConfirmacaoEntendimentoRegistrada::data,
                        ConfirmacaoEntendimentoRegistrada::versaoConfirmada,
                        ConfirmacaoEntendimentoRegistrada::escopo)
                .containsExactly(responsavel, OffsetDateTime.parse("2026-08-27T12:30:00-03:00"),
                        2, "Confirmacao do entendimento apresentado.");
        assertThat(servico.historico().avaliacoes()).containsExactly(avaliacao);
        assertThat(servico.historico().confirmacoes()).containsExactly(confirmacao);
    }

    @Test
    void deveRecusarAvaliacaoQuandoPoliticaAplicavelNaoEstiverHabilitada() {
        ComandoRegistrarAvaliacao comando = new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T13:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Tentativa sem politica aplicavel.",
                false
        );

        assertThatThrownBy(() -> servico.registrarAvaliacao(comando))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("avaliacao nao habilitada por politica institucional aplicavel.");
        assertThat(servico.historico().avaliacoes()).isEmpty();
    }
}

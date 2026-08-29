package br.gov.mppb.athena.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.SituacaoProjeto;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ServicoAvaliacaoConfirmacaoTest {

    private final ServicoAvaliacaoConfirmacao servico = new ServicoAvaliacaoConfirmacao();
    private final UsuarioInstitucional usuarioResponsavel = usuario("usuario-avaliador", "avaliador", "DITEC");
    private final IdentificadorUsuario responsavel = usuarioResponsavel.identificador();
    private final ProjetoInstitucional projeto = projetoComParticipante(usuarioResponsavel, PapelProjeto.RESPONSAVEL_TECNICO);
    private final ContextoAutorizacaoInstitucional contexto = new ContextoAutorizacaoInstitucional(
            usuarioResponsavel,
            Set.of()
    );
    private final ReferenciaAvaliacaoDocumental referencia = new ReferenciaAvaliacaoDocumental(
            new IdentificadorProjeto("ATHENA"),
            "DOC-ENTENDIMENTO",
            2,
            "ESP.REQ.006"
    );

    @Test
    void deveRegistrarFluxosComEntradasSaidasTransicoesEHistoricoObrigatorio() {
        AvaliacaoRegistrada avaliacao = servico.registrarAvaliacao(projeto, contexto, new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Item consistente com a linha de base.",
                true
        ));
        ComentarioRegistrado comentario = servico.registrarComentario(projeto, contexto, new ComandoRegistrarComentario(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:05:00-03:00"),
                "Comentario associado ao item avaliado.",
                DecisaoAvaliacao.APROVADO
        ));
        PedidoAjusteRegistrado pedido = servico.registrarPedidoAjuste(projeto, contexto, new ComandoRegistrarPedidoAjuste(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T09:10:00-03:00"),
                "Inconsistencia localizada em campo recebido."
        ));
        ConfirmacaoEntendimentoRegistrada confirmacao = servico.confirmarEntendimento(projeto, contexto,
                new ComandoConfirmarEntendimento(
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
        AvaliacaoRegistrada primeiraAvaliacao = servico.registrarAvaliacao(projeto, contexto,
                new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T10:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Primeira decisao individual.",
                true
        ));
        AvaliacaoRegistrada segundaAvaliacao = servico.registrarAvaliacao(projeto, contexto,
                new ComandoRegistrarAvaliacao(
                new ReferenciaAvaliacaoDocumental(referencia.projeto(), referencia.documento(), 2, "ESP.REQ.007"),
                responsavel,
                OffsetDateTime.parse("2026-08-27T10:10:00-03:00"),
                DecisaoAvaliacao.AJUSTE_SOLICITADO,
                "Segunda decisao individual.",
                true
        ));

        servico.confirmarEntendimento(projeto, contexto, new ComandoConfirmarEntendimento(
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
        PedidoAjusteRegistrado pedido = servico.registrarPedidoAjuste(projeto, contexto,
                new ComandoRegistrarPedidoAjuste(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T11:00:00-03:00"),
                "Campo recebido diverge da fonte aprovada."
        ));

        assertThat(pedido.documentoOriginal()).isEqualTo(referencia);
        assertThat(pedido.documentoOriginal().documento()).isEqualTo("DOC-ENTENDIMENTO");
        assertThat(pedido.documentoOriginal().versao()).isEqualTo(2);
        assertThat(pedido.responsavelTecnico()).isEqualTo(responsavel);
        assertThat(pedido.estado()).isEqualTo(EstadoSolicitacaoAjuste.PENDENTE);
        assertThat(servico.historico().pedidosAjuste()).containsExactly(pedido);
    }

    @Test
    void devePermitirResponsavelTecnicoAcolherSolicitacaoValida() {
        PedidoAjusteRegistrado pedido = registrarPedidoAjusteValido();

        PedidoAjusteRegistrado tratado = servico.tratarPedidoAjuste(projeto, contexto, pedido,
                new ComandoTratarPedidoAjuste(
                        responsavel,
                        OffsetDateTime.parse("2026-08-28T09:00:00-03:00"),
                        EstadoSolicitacaoAjuste.ACOLHIDA,
                        "Ajuste acolhido para composicao de nova versao."
                ));

        assertThat(tratado.documentoOriginal()).isEqualTo(pedido.documentoOriginal());
        assertThat(tratado.solicitante()).isEqualTo(pedido.solicitante());
        assertThat(tratado.data()).isEqualTo(pedido.data());
        assertThat(tratado.justificativa()).isEqualTo(pedido.justificativa());
        assertThat(tratado.responsavelTecnico()).isEqualTo(responsavel);
        assertThat(tratado.estado()).isEqualTo(EstadoSolicitacaoAjuste.ACOLHIDA);
        assertThat(tratado.tratadoEm()).isEqualTo(OffsetDateTime.parse("2026-08-28T09:00:00-03:00"));
        assertThat(tratado.permiteGerarNovaVersao()).isTrue();
        assertThat(servico.historico().pedidosAjuste()).containsExactly(tratado);
    }

    @Test
    void devePermitirNaoAcolhimentoComJustificativaObrigatoriaEPreservada() {
        PedidoAjusteRegistrado pedido = registrarPedidoAjusteValido();

        PedidoAjusteRegistrado tratado = servico.tratarPedidoAjuste(projeto, contexto, pedido,
                new ComandoTratarPedidoAjuste(
                        responsavel,
                        OffsetDateTime.parse("2026-08-28T10:00:00-03:00"),
                        EstadoSolicitacaoAjuste.NAO_ACOLHIDA,
                        "Documento atual permanece valido porque a solicitacao nao altera o entendimento aprovado."
                ));

        assertThat(tratado.estado()).isEqualTo(EstadoSolicitacaoAjuste.NAO_ACOLHIDA);
        assertThat(tratado.justificativaTratamento())
                .isEqualTo("Documento atual permanece valido porque a solicitacao nao altera o entendimento aprovado.");
        assertThat(tratado.permiteGerarNovaVersao()).isFalse();
        assertThat(tratado.documentoOriginal()).isEqualTo(referencia);
        assertThat(tratado.justificativa()).isEqualTo("Campo recebido diverge da fonte aprovada.");
        assertThat(servico.historico().pedidosAjuste()).containsExactly(tratado);
    }

    @Test
    void deveRecusarNaoAcolhimentoSemJustificativa() {
        PedidoAjusteRegistrado pedido = registrarPedidoAjusteValido();

        assertThatThrownBy(() -> servico.tratarPedidoAjuste(projeto, contexto, pedido,
                new ComandoTratarPedidoAjuste(
                        responsavel,
                        OffsetDateTime.parse("2026-08-28T10:30:00-03:00"),
                        EstadoSolicitacaoAjuste.NAO_ACOLHIDA,
                        " "
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("justificativa do nao acolhimento deve ser informado.");
        assertThat(servico.historico().pedidosAjuste()).containsExactly(pedido);
    }

    @Test
    void deveDestinarSolicitacaoAoResponsavelTecnicoMesmoQuandoSolicitanteEhAvaliador() {
        UsuarioInstitucional usuarioAvaliador = usuario("usuario-avaliador-externo", "avaliador.externo", "GABINETE");
        ProjetoInstitucional projetoComAvaliador = projetoComParticipantes(
                Set.of(
                        new ParticipanteProjeto(usuarioResponsavel, PapelProjeto.RESPONSAVEL_TECNICO),
                        new ParticipanteProjeto(usuarioAvaliador, PapelProjeto.AVALIADOR)
                ),
                usuarioResponsavel
        );
        ContextoAutorizacaoInstitucional contextoAvaliador = new ContextoAutorizacaoInstitucional(
                usuarioAvaliador,
                Set.of()
        );

        PedidoAjusteRegistrado pedido = servico.registrarPedidoAjuste(projetoComAvaliador, contextoAvaliador,
                new ComandoRegistrarPedidoAjuste(
                        referencia,
                        usuarioAvaliador.identificador(),
                        OffsetDateTime.parse("2026-08-28T11:00:00-03:00"),
                        "Solicitacao valida sem fila externa."
                ));

        assertThat(pedido.solicitante()).isEqualTo(usuarioAvaliador.identificador());
        assertThat(pedido.responsavelTecnico()).isEqualTo(responsavel);
        assertThat(pedido.estado()).isEqualTo(EstadoSolicitacaoAjuste.PENDENTE);
    }

    @Test
    void deveManterSolicitacaoAntigaTratavelSemExpiracaoAutomatica() {
        PedidoAjusteRegistrado pedido = servico.registrarPedidoAjuste(projeto, contexto,
                new ComandoRegistrarPedidoAjuste(
                        referencia,
                        responsavel,
                        OffsetDateTime.parse("2026-08-01T09:00:00-03:00"),
                        "Solicitacao registrada sem regra temporal automatica."
                ));

        PedidoAjusteRegistrado tratado = servico.tratarPedidoAjuste(projeto, contexto, pedido,
                new ComandoTratarPedidoAjuste(
                        responsavel,
                        OffsetDateTime.parse("2026-08-28T11:30:00-03:00"),
                        EstadoSolicitacaoAjuste.ACOLHIDA,
                        null
                ));

        assertThat(tratado.estado()).isEqualTo(EstadoSolicitacaoAjuste.ACOLHIDA);
        assertThat(tratado.data()).isEqualTo(OffsetDateTime.parse("2026-08-01T09:00:00-03:00"));
        assertThat(tratado.tratadoEm()).isEqualTo(OffsetDateTime.parse("2026-08-28T11:30:00-03:00"));
        assertThat(servico.historico().pedidosAjuste()).containsExactly(tratado);
    }

    @Test
    void deveRegistrarConfirmacaoSemSubstituirAvaliacoesIndividuais() {
        AvaliacaoRegistrada avaliacao = servico.registrarAvaliacao(projeto, contexto, new ComandoRegistrarAvaliacao(
                referencia,
                responsavel,
                OffsetDateTime.parse("2026-08-27T12:00:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Avaliacao individual preservada.",
                true
        ));

        ConfirmacaoEntendimentoRegistrada confirmacao = servico.confirmarEntendimento(projeto, contexto,
                new ComandoConfirmarEntendimento(
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

        assertThatThrownBy(() -> servico.registrarAvaliacao(projeto, contexto, comando))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("avaliacao nao habilitada por politica institucional aplicavel.");
        assertThat(servico.historico().avaliacoes()).isEmpty();
    }

    @Test
    void deveNegarOperacaoProtegidaQuandoUsuarioNaoTemVinculoOuAutorizacao() {
        UsuarioInstitucional usuarioSemVinculo = usuario("usuario-sem-vinculo", "sem.vinculo", "GABINETE");
        ContextoAutorizacaoInstitucional contextoSemPermissao = new ContextoAutorizacaoInstitucional(
                usuarioSemVinculo,
                Set.of()
        );
        ComandoRegistrarAvaliacao comando = new ComandoRegistrarAvaliacao(
                referencia,
                usuarioSemVinculo.identificador(),
                OffsetDateTime.parse("2026-08-27T13:30:00-03:00"),
                DecisaoAvaliacao.APROVADO,
                "Tentativa sem autorizacao.",
                true
        );

        assertThatThrownBy(() -> servico.registrarAvaliacao(projeto, contextoSemPermissao, comando))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("operacao protegida nao autorizada");
        assertThat(servico.historico().avaliacoes()).isEmpty();
    }

    private static ProjetoInstitucional projetoComParticipante(UsuarioInstitucional usuario, PapelProjeto papel) {
        return projetoComParticipantes(Set.of(new ParticipanteProjeto(usuario, papel)), usuario);
    }

    private static ProjetoInstitucional projetoComParticipantes(
            Set<ParticipanteProjeto> participantes,
            UsuarioInstitucional criador
    ) {
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                participantes,
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                criador
        );
    }

    private static UsuarioInstitucional usuario(String id, String login, String unidade) {
        return new UsuarioInstitucional(
                new IdentificadorUsuario(id),
                "Usuario Institucional",
                login,
                unidade
        );
    }

    private PedidoAjusteRegistrado registrarPedidoAjusteValido() {
        return servico.registrarPedidoAjuste(projeto, contexto,
                new ComandoRegistrarPedidoAjuste(
                        referencia,
                        responsavel,
                        OffsetDateTime.parse("2026-08-27T11:00:00-03:00"),
                        "Campo recebido diverge da fonte aprovada."
                ));
    }
}

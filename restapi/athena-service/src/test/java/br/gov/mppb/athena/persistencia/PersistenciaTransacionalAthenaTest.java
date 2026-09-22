package br.gov.mppb.athena.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.ItemDocumental;
import br.gov.mppb.athena.dominio.MetadadosDocumento;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.PermissaoDocumento;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.RastreabilidadeDocumento;
import br.gov.mppb.athena.dominio.RelacaoDocumental;
import br.gov.mppb.athena.dominio.SituacaoProjeto;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import br.gov.mppb.athena.integracao.ContratoIntegracaoDocumentalExterna;
import br.gov.mppb.athena.integracao.PedidoIngestaoDocumentoExterno;
import br.gov.mppb.athena.integracao.ResultadoIngestaoPersistente;
import br.gov.mppb.athena.integracao.ServicoIngestaoDocumentalPersistente;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

class PersistenciaTransacionalAthenaTest {

    private JdbcTemplate jdbc;
    private PersistenciaTransacionalAthena persistencia;

    @BeforeEach
    void prepararBancoExecutavel() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                        + ";INIT=CREATE SCHEMA IF NOT EXISTS athena",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(dataSource);
        persistencia = new PersistenciaTransacionalAthena(jdbc, new DataSourceTransactionManager(dataSource));
        criarEsquemaExecutavelDeTeste();
    }

    @Test
    void devePersistirConsultarEAtualizarHistoricoDocumentalEmTransacao() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T10:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, versaoUmId, itemUmId, 1, "conteudo aprovado", agora
        ));
        inserirNovaVersao(idDocumento, versaoUmId, versaoDoisId, itemDoisId, idUsuario, agora.plusHours(1));

        persistencia.atualizarSituacaoProjeto(idProjeto, "EM_REVISAO", idUsuario, agora.plusHours(2));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemDoisId,
                idUsuario,
                TipoInteracaoHumana.CONFIRMACAO,
                "CONFIRMADO",
                "conteudo conferido",
                "historico preservado",
                agora.plusHours(3),
                UUID.randomUUID()
        ));

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(idDocumento);

        assertThat(historico.projeto().situacao()).isEqualTo("EM_REVISAO");
        assertThat(historico.versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero)
                .containsExactly(1, 2);
        assertThat(historico.versoes())
                .extracting(DadosVersaoDocumentoPersistente::conteudoOriginal)
                .containsExactly("conteudo aprovado", "conteudo revisado");
        assertThat(historico.itens())
                .extracting(DadosItemDocumentalPersistente::identificador)
                .containsExactly("ITEM-1", "ITEM-2");
        assertThat(contar("athena.confirmacao")).isOne();
        assertThat(contar("athena.eventoauditoria")).isEqualTo(2);
    }

    @Test
    void deveExecutarRollbackQuandoRegistroHistoricoViolaRestricaoPersistente() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID idVersao = UUID.randomUUID();
        UUID idItem = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T11:00:00-03:00");

        RegistroHistoricoDocumental registro = registro(
                idUsuario, idProjeto, idDocumento, idVersao, idItem, 0, "conteudo invalido", agora
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> persistencia.registrarHistoricoDocumental(registro)
        ).isInstanceOf(RuntimeException.class);

        assertThat(contar("athena.usuarioinstitucional")).isZero();
        assertThat(contar("athena.projeto")).isZero();
        assertThat(contar("athena.documento")).isZero();
        assertThat(contar("athena.versaodocumento")).isZero();
    }

    @Test
    void deveManterDdlEstruturalSeparadoDaPersistenciaExecutavel() {
        assertThat(PersistenciaTransacionalAthena.class.getPackageName())
                .isEqualTo("br.gov.mppb.athena.persistencia");
        assertThat(contar("information_schema.tables")).isGreaterThan(0);
        assertThat(jdbc.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'athena'
                 ORDER BY table_name
                """, String.class))
                .contains("documento", "versaodocumento", "itemdocumental", "eventoauditoria");
    }

    @Test
    void deveConectarIngestaoExternaAPersistenciaTransacionalPreservandoHistorico() {
        UsuarioInstitucional administrador = usuario("usuario-admin", "admin.ditec", "DITEC");
        ProjetoInstitucional projeto = projeto(administrador);
        ContextoAutorizacaoInstitucional contexto = new ContextoAutorizacaoInstitucional(
                administrador,
                Set.of(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)
        );
        ServicoIngestaoDocumentalPersistente servico = new ServicoIngestaoDocumentalPersistente(persistencia);

        ResultadoIngestaoPersistente primeira = servico.registrarPrimeiraVersao(
                projeto,
                contexto,
                pedido(FormatoDocumento.MARKDOWN, "entrada.yaml", "conteudo original em MARKDOWN")
        );
        DadosItemDocumentalPersistente itemPrimeiraVersao = persistencia
                .consultarHistoricoDocumento(primeira.idDocumentoPersistente())
                .itens()
                .get(0);
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemPrimeiraVersao.id(),
                UUID.randomUUID(),
                TipoInteracaoHumana.CONFIRMACAO,
                "CONFIRMADO",
                "confirmacao formal",
                "conteudo conferido",
                OffsetDateTime.parse("2026-08-25T10:25:30-03:00"),
                UUID.randomUUID()
        ));
        ResultadoIngestaoPersistente segunda = servico.registrarNovaVersao(
                projeto,
                contexto,
                primeira.documento(),
                pedido(FormatoDocumento.JSON, "entrada.md", "conteudo original em JSON")
        );

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(segunda.idDocumentoPersistente());
        ContratoIntegracaoDocumentalExterna contrato = servico.contratoDependenciaExternaGovernada();

        assertThat(primeira.documento().versoes()).singleElement()
                .satisfies(versao -> assertThat(versao.conteudoOriginal()).isEqualTo("conteudo original em MARKDOWN"));
        assertThat(segunda.documento().versoes()).hasSize(2);
        assertThat(historico.versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero, DadosVersaoDocumentoPersistente::formato,
                        DadosVersaoDocumentoPersistente::conteudoOriginal)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "MARKDOWN", "conteudo original em MARKDOWN"),
                        org.assertj.core.groups.Tuple.tuple(2, "JSON", "conteudo original em JSON")
                );
        assertThat(historico.itens())
                .extracting(DadosItemDocumentalPersistente::identificador)
                .containsExactly("SOL.INTEGRACAO.001", "SOL.INTEGRACAO.001");
        assertThat(contrato.dependenciaExternaDisponivel()).isFalse();
        assertThat(contrato.tratamentoIndisponibilidade()).contains("contrato de integracao pendente");
        assertThat(contar("athena.eventoauditoria")).isEqualTo(3);
    }

    @Test
    void deveAtualizarReferenciaNaMesmaTransacaoEPreservarHistoricoEConfirmacao() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-09-17T14:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, versaoUmId, UUID.randomUUID(), 1, "versao historica", agora
        ));
        persistencia.confirmarVersaoDocumental(versaoUmId);
        persistencia.registrarNovaVersaoDocumental(
                idProjeto,
                idUsuario,
                new DadosVersaoDocumentoPersistente(
                        versaoDoisId, idDocumento, 2, "MARKDOWN", "versao corrente", "hash-2",
                        agora.plusHours(1), idUsuario, "teste H2", versaoUmId, false, null
                ),
                List.of(new DadosItemDocumentalPersistente(
                        UUID.randomUUID(), versaoDoisId, "ITEM-2", "Item dois", "conteudo dois", 1,
                        List.of("ESP.REQ.010")
                )),
                agora.plusHours(1),
                UUID.randomUUID()
        );

        ReferenciaVersaoAtualPersistente referencia = persistencia.consultarReferenciaVersaoAtual(idDocumento);
        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(idDocumento);

        assertThat(referencia.idVersaoAtual()).isEqualTo(versaoDoisId);
        assertThat(referencia.versaoControle()).isEqualTo(2);
        assertThat(historico.versoes()).extracting(DadosVersaoDocumentoPersistente::id)
                .containsExactly(versaoUmId, versaoDoisId);
        assertThat(historico.versoes()).extracting(DadosVersaoDocumentoPersistente::confirmada)
                .containsExactly(true, false);
    }

    @Test
    void deveReverterVersaoItensEReferenciaQuandoRecebimentoFalhar() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemDuplicado = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-09-17T15:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, versaoUmId, UUID.randomUUID(), 1, "versao preservada", agora
        ));
        persistencia.confirmarVersaoDocumental(versaoUmId);
        DadosVersaoDocumentoPersistente versaoDois = new DadosVersaoDocumentoPersistente(
                versaoDoisId, idDocumento, 2, "MARKDOWN", "versao que falhara", "hash-2",
                agora.plusHours(1), idUsuario, "teste H2", versaoUmId, false, null
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                itemDuplicado, versaoDoisId, "ITEM-2", "Item dois", "conteudo", 1, List.of("ESP.REQ.010")
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> persistencia.registrarNovaVersaoDocumental(
                idProjeto, idUsuario, versaoDois, List.of(item, item), agora.plusHours(1), UUID.randomUUID()
        )).isInstanceOf(RuntimeException.class);

        ReferenciaVersaoAtualPersistente referencia = persistencia.consultarReferenciaVersaoAtual(idDocumento);
        assertThat(referencia.idVersaoAtual()).isEqualTo(versaoUmId);
        assertThat(referencia.versaoControle()).isOne();
        assertThat(persistencia.listarVersoesDocumento(idDocumento))
                .extracting(DadosVersaoDocumentoPersistente::id)
                .containsExactly(versaoUmId);
    }

    @Test
    void deveRejeitarReferenciaDeOutroDocumentoEAtualizacaoConcorrenteObsoleta() {
        UUID idUsuarioA = UUID.randomUUID();
        UUID idProjetoA = UUID.randomUUID();
        UUID idDocumentoA = UUID.randomUUID();
        UUID idVersaoA = UUID.randomUUID();
        UUID idDocumentoB = UUID.randomUUID();
        UUID idVersaoB = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-09-17T16:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuarioA, idProjetoA, idDocumentoA, idVersaoA, UUID.randomUUID(), 1, "documento A", agora
        ));
        jdbc.update("""
                INSERT INTO athena.documento
                    (id, identificador, id_projeto, titulo, grupo, categoria, finalidade,
                     apresentacao, datacriacao, id_usuariocriacao, id_versao_atual, versao_controle)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, idDocumentoB, "DOC-B", idProjetoA, "Documento B", "Grupo", "Categoria",
                "Finalidade", "Apresentacao", agora, idUsuarioA, idVersaoB, 1);
        jdbc.update("""
                INSERT INTO athena.versaodocumento
                    (id, id_documento, numero, formato, conteudooriginal, hashconteudo,
                     datarecebimento, id_usuariorecebimento, origemexterna, confirmada)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, idVersaoB, idDocumentoB, 1, "MARKDOWN", "documento B", "hash-b",
                agora, idUsuarioA, "teste H2", false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> persistencia.atualizarReferenciaVersaoAtual(
                idDocumentoA, idVersaoB, idVersaoA, 1
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proprio documento");

        persistencia.atualizarReferenciaVersaoAtual(idDocumentoA, idVersaoA, idVersaoA, 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> persistencia.atualizarReferenciaVersaoAtual(
                idDocumentoA, idVersaoA, idVersaoA, 1
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outra operacao");
        assertThat(persistencia.consultarReferenciaVersaoAtual(idDocumentoA).versaoControle()).isEqualTo(2);
    }

    @Test
    void deveMarcarVersaoConfirmadaComoImutavelEPreservarConteudoOriginal() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID idVersao = UUID.randomUUID();
        UUID idItem = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T12:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, idVersao, idItem, 1, "conteudo formalmente confirmado", agora
        ));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                idItem,
                idUsuario,
                TipoInteracaoHumana.CONFIRMACAO,
                "CONFIRMADO",
                "confirmacao formal",
                "escopo confirmado",
                agora.plusMinutes(5),
                UUID.randomUUID()
        ));

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(idDocumento);

        assertThat(historico.versoes()).singleElement()
                .satisfies(versao -> {
                    assertThat(versao.confirmada()).isTrue();
                    assertThat(versao.conteudoOriginal()).isEqualTo("conteudo formalmente confirmado");
                    assertThat(versao.idVersaoAnterior()).isNull();
                });
    }

    @Test
    void deveExigirVinculoComVersaoAnteriorParaNovaVersaoPersistente() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T13:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, versaoUmId, itemUmId, 1, "conteudo preservado", agora
        ));
        DadosVersaoDocumentoPersistente versaoSemPredecessora = new DadosVersaoDocumentoPersistente(
                versaoDoisId, idDocumento, 2, "MARKDOWN", "conteudo revisado", "hash-2",
                agora.plusHours(1), idUsuario, "origem externa", null, false, null
        );
        DadosItemDocumentalPersistente itemDois = new DadosItemDocumentalPersistente(
                itemDoisId, versaoDoisId, "ITEM-2", "Item versionado", "conteudo revisado", 1, List.of("ESP.REQ.010")
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> persistencia.registrarNovaVersaoDocumental(
                idProjeto, idUsuario, versaoSemPredecessora, List.of(itemDois), agora.plusHours(1), UUID.randomUUID()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versao anterior");

        assertThat(persistencia.consultarHistoricoDocumento(idDocumento).versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero)
                .containsExactly(1);
    }

    @Test
    void deveAceitarNovaVersaoQuandoAnteriorExisteMasAindaNaoFoiConfirmada() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T13:30:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, versaoUmId, itemUmId, 1, "conteudo ainda nao confirmado", agora
        ));
        DadosVersaoDocumentoPersistente versaoDois = new DadosVersaoDocumentoPersistente(
                versaoDoisId, idDocumento, 2, "MARKDOWN", "conteudo revisado", "hash-2",
                agora.plusHours(1), idUsuario, "origem externa", versaoUmId, false, null
        );
        DadosItemDocumentalPersistente itemDois = new DadosItemDocumentalPersistente(
                itemDoisId, versaoDoisId, "ITEM-2", "Item versionado", "conteudo revisado", 1, List.of("ESP.REQ.010")
        );

        persistencia.registrarNovaVersaoDocumental(
                idProjeto, idUsuario, versaoDois, List.of(itemDois), agora.plusHours(1), UUID.randomUUID()
        );

        assertThat(persistencia.consultarHistoricoDocumento(idDocumento).versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero)
                .containsExactly(1, 2);
        assertThat(persistencia.consultarHistoricoDocumento(idDocumento).versoes().get(0).confirmada())
                .isFalse();
        assertThat(persistencia.consultarHistoricoDocumento(idDocumento).versoes().get(1).idVersaoAnterior())
                .isEqualTo(versaoUmId);
    }

    @Test
    void deveCriarNovaVersaoVinculadaSemSobrescreverVersaoConfirmadaAnterior() {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        UUID pedidoAjusteId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T14:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                idUsuario, idProjeto, idDocumento, versaoUmId, itemUmId, 1, "conteudo aprovado imutavel", agora
        ));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemUmId,
                idUsuario,
                TipoInteracaoHumana.CONFIRMACAO,
                "CONFIRMADO",
                "confirmacao formal",
                "escopo confirmado",
                agora.plusMinutes(5),
                UUID.randomUUID()
        ));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                pedidoAjusteId,
                itemUmId,
                idUsuario,
                TipoInteracaoHumana.PEDIDO_AJUSTE,
                "ACOLHIDO",
                "ajuste acolhido",
                "ajuste acolhido",
                agora.plusMinutes(10),
                UUID.randomUUID()
        ));
        DadosVersaoDocumentoPersistente versaoDois = new DadosVersaoDocumentoPersistente(
                versaoDoisId, idDocumento, 2, "MARKDOWN", "conteudo alterado em nova versao", "hash-2",
                agora.plusHours(1), idUsuario, "origem externa", versaoUmId, false, pedidoAjusteId
        );
        DadosItemDocumentalPersistente itemDois = new DadosItemDocumentalPersistente(
                itemDoisId, versaoDoisId, "ITEM-2", "Item versionado", "conteudo alterado em nova versao", 1,
                List.of("ESP.REQ.010", "ESP.ACEITE.012")
        );

        persistencia.registrarNovaVersaoDocumental(
                idProjeto, idUsuario, versaoDois, List.of(itemDois), agora.plusHours(1), UUID.randomUUID()
        );

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(idDocumento);
        assertThat(historico.versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero, DadosVersaoDocumentoPersistente::conteudoOriginal)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "conteudo aprovado imutavel"),
                        org.assertj.core.groups.Tuple.tuple(2, "conteudo alterado em nova versao")
                );
        assertThat(historico.versoes().get(0).confirmada()).isTrue();
        assertThat(historico.versoes().get(1).idVersaoAnterior()).isEqualTo(versaoUmId);
        assertThat(historico.versoes().get(1).idPedidoAjusteOrigem()).isEqualTo(pedidoAjusteId);
        assertThat(historico.itens())
                .extracting(DadosItemDocumentalPersistente::conteudo)
                .containsExactly("conteudo aprovado imutavel", "conteudo alterado em nova versao");
        assertThat(jdbc.queryForObject(
                "SELECT id_versao_resultante FROM athena.pedidoajuste WHERE id = ?",
                UUID.class,
                pedidoAjusteId
        )).isEqualTo(versaoDoisId);
    }

    private RegistroHistoricoDocumental registro(
            UUID idUsuario,
            UUID idProjeto,
            UUID idDocumento,
            UUID idVersao,
            UUID idItem,
            int numeroVersao,
            String conteudo,
            OffsetDateTime agora
    ) {
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                idUsuario, "USR-001", "Responsavel Tecnico", "responsavel", "TI"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                idProjeto, "ATHENA", "Athena", "Gestao documental", "ATIVO", agora, idUsuario
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(), idProjeto, idUsuario, "RESPONSAVEL_TECNICO", "TI", agora, idUsuario
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                idDocumento, "DOC-001", idProjeto, "Linha de Base", "BACKLOG", "REQUISITO",
                "Preservar historico documental", "Markdown", agora, idUsuario
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                idVersao, idDocumento, numeroVersao, "MARKDOWN", conteudo, "hash-" + numeroVersao,
                agora, idUsuario, "origem externa"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                idItem, idVersao, "ITEM-" + numeroVersao, "Item versionado", conteudo, 1, List.of("BL.US.005")
        );
        return new RegistroHistoricoDocumental(
                usuario, projeto, vinculo, documento, versao, List.of(item), agora, UUID.randomUUID()
        );
    }

    private void inserirNovaVersao(
            UUID idDocumento,
            UUID idVersaoAnterior,
            UUID idVersao,
            UUID idItem,
            UUID idUsuario,
            OffsetDateTime dataRecebimento
    ) {
        jdbc.update("""
                INSERT INTO athena.versaodocumento
                    (id, id_documento, numero, formato, conteudooriginal, hashconteudo,
                     datarecebimento, id_usuariorecebimento, origemexterna, id_versao_anterior)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, idVersao, idDocumento, 2, "MARKDOWN", "conteudo revisado", "hash-2",
                dataRecebimento, idUsuario, "origem externa", idVersaoAnterior);
        jdbc.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO athena.itemdocumental
                        (id, id_versao, identificador, titulo, conteudo, ordem, fontesrastreabilidade)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
            ps.setObject(1, idItem);
            ps.setObject(2, idVersao);
            ps.setString(3, "ITEM-2");
            ps.setString(4, "Item versionado");
            ps.setString(5, "conteudo revisado");
            ps.setInt(6, 1);
            ps.setArray(7, connection.createArrayOf("VARCHAR", new String[] {"BL.US.006"}));
            return ps;
        });
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
    }

    private void criarEsquemaExecutavelDeTeste() {
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS athena");
        jdbc.execute("""
                CREATE TABLE athena.usuarioinstitucional (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    login VARCHAR(120) NOT NULL UNIQUE,
                    unidade VARCHAR(80) NOT NULL,
                    datacadastro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.projeto (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(80) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    descricao CLOB NOT NULL,
                    situacao VARCHAR(40) NOT NULL,
                    id_unidade_demandante INTEGER,
                    nome_unidade_demandante VARCHAR(200),
                    id_unidade_desenvolvedora INTEGER,
                    nome_unidade_desenvolvedora VARCHAR(200),
                    datacriacao TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuariocriacao UUID NOT NULL,
                    dataalteracao TIMESTAMP WITH TIME ZONE,
                    id_usuarioalteracao UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.vinculoprojeto (
                    id UUID PRIMARY KEY,
                    id_projeto UUID NOT NULL,
                    id_usuario UUID NOT NULL,
                    papel VARCHAR(60) NOT NULL,
                    unidade VARCHAR(80) NOT NULL,
                    datainicio TIMESTAMP WITH TIME ZONE NOT NULL,
                    datafim TIMESTAMP WITH TIME ZONE,
                    id_usuarioregistro UUID NOT NULL,
                    UNIQUE (id_projeto, id_usuario, papel, datainicio)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.documento (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL,
                    id_projeto UUID NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    grupo VARCHAR(120) NOT NULL,
                    categoria VARCHAR(120) NOT NULL,
                    finalidade VARCHAR(240) NOT NULL,
                    apresentacao VARCHAR(240) NOT NULL,
                    datacriacao TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuariocriacao UUID NOT NULL,
                    id_versao_atual UUID,
                    versao_controle BIGINT NOT NULL DEFAULT 0,
                    UNIQUE (id_projeto, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.versaodocumento (
                    id UUID PRIMARY KEY,
                    id_documento UUID NOT NULL,
                    numero INTEGER NOT NULL CHECK (numero > 0),
                    formato VARCHAR(40) NOT NULL,
                    conteudooriginal CLOB NOT NULL,
                    hashconteudo VARCHAR(128) NOT NULL,
                    datarecebimento TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuariorecebimento UUID NOT NULL,
                    origemexterna VARCHAR(240),
                    id_versao_anterior UUID,
                    confirmada BOOLEAN NOT NULL DEFAULT FALSE,
                    id_pedido_ajuste_origem UUID,
                    original_binario BINARY VARYING,
                    original_tamanho BIGINT,
                    original_tipo_midia VARCHAR(120),
                    original_codificacao VARCHAR(20),
                    hash_metadados VARCHAR(64),
                    chave_idempotencia VARCHAR(64),
                    CHECK ((numero = 1 AND id_versao_anterior IS NULL) OR (numero > 1 AND id_versao_anterior IS NOT NULL)),
                    UNIQUE (id_documento, numero)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.itemdocumental (
                    id UUID PRIMARY KEY,
                    id_versao UUID NOT NULL,
                    identificador VARCHAR(160) NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    conteudo CLOB NOT NULL,
                    ordem INTEGER NOT NULL CHECK (ordem > 0),
                    fontesrastreabilidade VARCHAR ARRAY NOT NULL,
                    UNIQUE (id_versao, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.avaliacao (
                    id UUID PRIMARY KEY,
                    id_item UUID NOT NULL,
                    id_responsavel UUID NOT NULL,
                    decisao VARCHAR(80) NOT NULL,
                    justificativa CLOB NOT NULL,
                    dataavaliacao TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.comentario (
                    id UUID PRIMARY KEY,
                    id_item UUID NOT NULL,
                    id_autor UUID NOT NULL,
                    conteudo CLOB NOT NULL,
                    dataregistro TIMESTAMP WITH TIME ZONE NOT NULL,
                    origem VARCHAR(120) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.pedidoajuste (
                    id UUID PRIMARY KEY,
                    id_item UUID NOT NULL,
                    id_solicitante UUID NOT NULL,
                    descricao CLOB NOT NULL,
                    justificativa CLOB NOT NULL,
                    situacao VARCHAR(60) NOT NULL,
                    datasolicitacao TIMESTAMP WITH TIME ZONE NOT NULL,
                    dataconclusao TIMESTAMP WITH TIME ZONE,
                    responsaveltecnico VARCHAR(120) NOT NULL,
                    datatratamento TIMESTAMP WITH TIME ZONE,
                    decisaotratamento VARCHAR(60),
                    justificativatratamento CLOB,
                    id_versao_resultante UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.confirmacao (
                    id UUID PRIMARY KEY,
                    id_item UUID NOT NULL,
                    id_responsavel UUID NOT NULL,
                    decisao VARCHAR(80) NOT NULL,
                    justificativa CLOB NOT NULL,
                    dataconfirmacao TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.eventoauditoria (
                    id UUID PRIMARY KEY,
                    id_projeto UUID,
                    id_documento UUID,
                    id_versao UUID,
                    id_item UUID,
                    id_usuario UUID,
                    tipo VARCHAR(100) NOT NULL,
                    decisao VARCHAR(100),
                    justificativa CLOB,
                    dados CLOB DEFAULT '{}' NOT NULL,
                    dataocorrencia TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuarioregistro UUID NOT NULL
                )
                """);
    }

    private static PedidoIngestaoDocumentoExterno pedido(
            FormatoDocumento formato,
            String nomeFisicoArquivo,
            String conteudoOriginal
    ) {
        return new PedidoIngestaoDocumentoExterno(
                "DOC-ESPEC-001",
                new IdentificadorProjeto("ATHENA"),
                "Linha de base de especificacao",
                new MetadadosDocumento(
                        "Especificacao",
                        "Linha de base",
                        "Consulta institucional",
                        "Arvore documental",
                        Set.of(PermissaoDocumento.CONSULTAR, PermissaoDocumento.AVALIAR),
                        Set.of(new RelacaoDocumental("rastreia", "BL.AC.004")),
                        new RastreabilidadeDocumento(Set.of("BL.AC.004", "SOL.INTEGRACAO.001"))
                ),
                formato,
                conteudoOriginal,
                List.of(new ItemDocumental(
                        "SOL.INTEGRACAO.001",
                        "Ingestao de documentos externos",
                        "Representacao interna para exibicao.",
                        Set.of(new RelacaoDocumental("rastreia", "BL.AC.004")),
                        new RastreabilidadeDocumento(Set.of("SOL.INTEGRACAO.001"))
                )),
                OffsetDateTime.parse("2026-08-25T10:20:30-03:00"),
                nomeFisicoArquivo
        );
    }

    private static ProjetoInstitucional projeto(UsuarioInstitucional usuario) {
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(usuario, PapelProjeto.RESPONSAVEL_TECNICO)),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                usuario
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
}

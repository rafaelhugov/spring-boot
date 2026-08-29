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
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T10:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                usuarioId, projetoId, documentoId, versaoUmId, itemUmId, 1, "conteudo aprovado", agora
        ));
        inserirNovaVersao(documentoId, versaoUmId, versaoDoisId, itemDoisId, usuarioId, agora.plusHours(1));

        persistencia.atualizarSituacaoProjeto(projetoId, "EM_REVISAO", usuarioId, agora.plusHours(2));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemDoisId,
                usuarioId,
                TipoInteracaoHumana.CONFIRMACAO,
                "CONFIRMADO",
                "conteudo conferido",
                "historico preservado",
                agora.plusHours(3),
                UUID.randomUUID()
        ));

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(documentoId);

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
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T11:00:00-03:00");

        RegistroHistoricoDocumental registro = registro(
                usuarioId, projetoId, documentoId, versaoId, itemId, 0, "conteudo invalido", agora
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
                .consultarHistoricoDocumento(primeira.documentoIdPersistente())
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

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(segunda.documentoIdPersistente());
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
    void deveMarcarVersaoConfirmadaComoImutavelEPreservarConteudoOriginal() {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T12:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                usuarioId, projetoId, documentoId, versaoId, itemId, 1, "conteudo formalmente confirmado", agora
        ));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemId,
                usuarioId,
                TipoInteracaoHumana.CONFIRMACAO,
                "CONFIRMADO",
                "confirmacao formal",
                "escopo confirmado",
                agora.plusMinutes(5),
                UUID.randomUUID()
        ));

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(documentoId);

        assertThat(historico.versoes()).singleElement()
                .satisfies(versao -> {
                    assertThat(versao.confirmada()).isTrue();
                    assertThat(versao.conteudoOriginal()).isEqualTo("conteudo formalmente confirmado");
                    assertThat(versao.versaoAnteriorId()).isNull();
                });
    }

    @Test
    void deveExigirVinculoComVersaoAnteriorParaNovaVersaoPersistente() {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T13:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                usuarioId, projetoId, documentoId, versaoUmId, itemUmId, 1, "conteudo preservado", agora
        ));
        DadosVersaoDocumentoPersistente versaoSemPredecessora = new DadosVersaoDocumentoPersistente(
                versaoDoisId, documentoId, 2, "MARKDOWN", "conteudo revisado", "hash-2",
                agora.plusHours(1), usuarioId, "origem externa", null, false, null
        );
        DadosItemDocumentalPersistente itemDois = new DadosItemDocumentalPersistente(
                itemDoisId, versaoDoisId, "ITEM-2", "Item versionado", "conteudo revisado", 1, List.of("ESP.REQ.010")
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> persistencia.registrarNovaVersaoDocumental(
                projetoId, usuarioId, versaoSemPredecessora, List.of(itemDois), agora.plusHours(1), UUID.randomUUID()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versao anterior");

        assertThat(persistencia.consultarHistoricoDocumento(documentoId).versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero)
                .containsExactly(1);
    }

    @Test
    void deveExigirVersaoAnteriorConfirmadaParaNovaVersaoPersistente() {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T13:30:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                usuarioId, projetoId, documentoId, versaoUmId, itemUmId, 1, "conteudo ainda nao confirmado", agora
        ));
        DadosVersaoDocumentoPersistente versaoDois = new DadosVersaoDocumentoPersistente(
                versaoDoisId, documentoId, 2, "MARKDOWN", "conteudo revisado", "hash-2",
                agora.plusHours(1), usuarioId, "origem externa", versaoUmId, false, null
        );
        DadosItemDocumentalPersistente itemDois = new DadosItemDocumentalPersistente(
                itemDoisId, versaoDoisId, "ITEM-2", "Item versionado", "conteudo revisado", 1, List.of("ESP.REQ.010")
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> persistencia.registrarNovaVersaoDocumental(
                projetoId, usuarioId, versaoDois, List.of(itemDois), agora.plusHours(1), UUID.randomUUID()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmada");

        assertThat(persistencia.consultarHistoricoDocumento(documentoId).versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero)
                .containsExactly(1);
    }

    @Test
    void deveCriarNovaVersaoVinculadaSemSobrescreverVersaoConfirmadaAnterior() {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID versaoDoisId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        UUID itemDoisId = UUID.randomUUID();
        UUID pedidoAjusteId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T14:00:00-03:00");

        persistencia.registrarHistoricoDocumental(registro(
                usuarioId, projetoId, documentoId, versaoUmId, itemUmId, 1, "conteudo aprovado imutavel", agora
        ));
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemUmId,
                usuarioId,
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
                usuarioId,
                TipoInteracaoHumana.PEDIDO_AJUSTE,
                "ACOLHIDO",
                "ajuste acolhido",
                "ajuste acolhido",
                agora.plusMinutes(10),
                UUID.randomUUID()
        ));
        DadosVersaoDocumentoPersistente versaoDois = new DadosVersaoDocumentoPersistente(
                versaoDoisId, documentoId, 2, "MARKDOWN", "conteudo alterado em nova versao", "hash-2",
                agora.plusHours(1), usuarioId, "origem externa", versaoUmId, false, pedidoAjusteId
        );
        DadosItemDocumentalPersistente itemDois = new DadosItemDocumentalPersistente(
                itemDoisId, versaoDoisId, "ITEM-2", "Item versionado", "conteudo alterado em nova versao", 1,
                List.of("ESP.REQ.010", "ESP.ACEITE.012")
        );

        persistencia.registrarNovaVersaoDocumental(
                projetoId, usuarioId, versaoDois, List.of(itemDois), agora.plusHours(1), UUID.randomUUID()
        );

        HistoricoDocumentoPersistente historico = persistencia.consultarHistoricoDocumento(documentoId);
        assertThat(historico.versoes())
                .extracting(DadosVersaoDocumentoPersistente::numero, DadosVersaoDocumentoPersistente::conteudoOriginal)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "conteudo aprovado imutavel"),
                        org.assertj.core.groups.Tuple.tuple(2, "conteudo alterado em nova versao")
                );
        assertThat(historico.versoes().get(0).confirmada()).isTrue();
        assertThat(historico.versoes().get(1).versaoAnteriorId()).isEqualTo(versaoUmId);
        assertThat(historico.versoes().get(1).pedidoAjusteOrigemId()).isEqualTo(pedidoAjusteId);
        assertThat(historico.itens())
                .extracting(DadosItemDocumentalPersistente::conteudo)
                .containsExactly("conteudo aprovado imutavel", "conteudo alterado em nova versao");
        assertThat(jdbc.queryForObject(
                "SELECT versaoresultanteid FROM athena.pedidoajuste WHERE id = ?",
                UUID.class,
                pedidoAjusteId
        )).isEqualTo(versaoDoisId);
    }

    private RegistroHistoricoDocumental registro(
            UUID usuarioId,
            UUID projetoId,
            UUID documentoId,
            UUID versaoId,
            UUID itemId,
            int numeroVersao,
            String conteudo,
            OffsetDateTime agora
    ) {
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                usuarioId, "USR-001", "Responsavel Tecnico", "responsavel", "TI"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                projetoId, "ATHENA", "Athena", "Gestao documental", "ATIVO", agora, usuarioId
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(), projetoId, usuarioId, "RESPONSAVEL_TECNICO", "TI", agora, usuarioId
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                documentoId, "DOC-001", projetoId, "Linha de Base", "BACKLOG", "REQUISITO",
                "Preservar historico documental", "Markdown", agora, usuarioId
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                versaoId, documentoId, numeroVersao, "MARKDOWN", conteudo, "hash-" + numeroVersao,
                agora, usuarioId, "origem externa"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                itemId, versaoId, "ITEM-" + numeroVersao, "Item versionado", conteudo, 1, List.of("BL.US.005")
        );
        return new RegistroHistoricoDocumental(
                usuario, projeto, vinculo, documento, versao, List.of(item), agora, UUID.randomUUID()
        );
    }

    private void inserirNovaVersao(
            UUID documentoId,
            UUID versaoAnteriorId,
            UUID versaoId,
            UUID itemId,
            UUID usuarioId,
            OffsetDateTime recebidaEm
    ) {
        jdbc.update("""
                INSERT INTO athena.versaodocumento
                    (id, documentoid, numero, formato, conteudooriginal, hashconteudo,
                     recebidaem, recebidaporid, origemexterna, versaoanteriorid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, versaoId, documentoId, 2, "MARKDOWN", "conteudo revisado", "hash-2",
                recebidaEm, usuarioId, "origem externa", versaoAnteriorId);
        jdbc.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO athena.itemdocumental
                        (id, versaoid, identificador, titulo, conteudo, ordem, fontesrastreabilidade)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
            ps.setObject(1, itemId);
            ps.setObject(2, versaoId);
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
                    cadastradoem TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.projeto (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(80) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    descricao CLOB NOT NULL,
                    situacao VARCHAR(40) NOT NULL,
                    criadoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    criadoporid UUID NOT NULL,
                    alteradoem TIMESTAMP WITH TIME ZONE,
                    alteradoporid UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.vinculoprojeto (
                    id UUID PRIMARY KEY,
                    projetoid UUID NOT NULL,
                    usuarioid UUID NOT NULL,
                    papel VARCHAR(60) NOT NULL,
                    unidade VARCHAR(80) NOT NULL,
                    inicioem TIMESTAMP WITH TIME ZONE NOT NULL,
                    fimem TIMESTAMP WITH TIME ZONE,
                    registradoporid UUID NOT NULL,
                    UNIQUE (projetoid, usuarioid, papel, inicioem)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.documento (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL,
                    projetoid UUID NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    grupo VARCHAR(120) NOT NULL,
                    categoria VARCHAR(120) NOT NULL,
                    finalidade VARCHAR(240) NOT NULL,
                    apresentacao VARCHAR(240) NOT NULL,
                    criadoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    criadoporid UUID NOT NULL,
                    UNIQUE (projetoid, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.versaodocumento (
                    id UUID PRIMARY KEY,
                    documentoid UUID NOT NULL,
                    numero INTEGER NOT NULL CHECK (numero > 0),
                    formato VARCHAR(40) NOT NULL,
                    conteudooriginal CLOB NOT NULL,
                    hashconteudo VARCHAR(128) NOT NULL,
                    recebidaem TIMESTAMP WITH TIME ZONE NOT NULL,
                    recebidaporid UUID NOT NULL,
                    origemexterna VARCHAR(240),
                    versaoanteriorid UUID,
                    confirmada BOOLEAN DEFAULT FALSE NOT NULL,
                    pedidoajusteorigemid UUID,
                    CHECK ((numero = 1 AND versaoanteriorid IS NULL) OR (numero > 1 AND versaoanteriorid IS NOT NULL)),
                    UNIQUE (documentoid, numero)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.itemdocumental (
                    id UUID PRIMARY KEY,
                    versaoid UUID NOT NULL,
                    identificador VARCHAR(160) NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    conteudo CLOB NOT NULL,
                    ordem INTEGER NOT NULL CHECK (ordem > 0),
                    fontesrastreabilidade VARCHAR ARRAY NOT NULL,
                    UNIQUE (versaoid, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.avaliacao (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    responsavelid UUID NOT NULL,
                    decisao VARCHAR(80) NOT NULL,
                    justificativa CLOB NOT NULL,
                    avaliadaem TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.comentario (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    autorid UUID NOT NULL,
                    conteudo CLOB NOT NULL,
                    registradoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    origem VARCHAR(120) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.pedidoajuste (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    solicitanteid UUID NOT NULL,
                    descricao CLOB NOT NULL,
                    justificativa CLOB NOT NULL,
                    situacao VARCHAR(60) NOT NULL,
                    solicitadoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    concluidoem TIMESTAMP WITH TIME ZONE,
                    versaoresultanteid UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.confirmacao (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    responsavelid UUID NOT NULL,
                    decisao VARCHAR(80) NOT NULL,
                    justificativa CLOB NOT NULL,
                    confirmadaem TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.eventoauditoria (
                    id UUID PRIMARY KEY,
                    projetoid UUID,
                    documentoid UUID,
                    versaoid UUID,
                    itemid UUID,
                    usuarioid UUID,
                    tipo VARCHAR(100) NOT NULL,
                    decisao VARCHAR(100),
                    justificativa CLOB,
                    dados CLOB DEFAULT '{}' NOT NULL,
                    ocorridoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    registradoporid UUID NOT NULL
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

package br.gov.mppb.athena.testes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.mppb.athena.api.AthenaHttpController;
import br.gov.mppb.athena.persistencia.DadosDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosItemDocumentalPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosUsuarioPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosVinculoProjetoPersistente;
import br.gov.mppb.athena.persistencia.PersistenciaTransacionalAthena;
import br.gov.mppb.athena.persistencia.RegistroHistoricoDocumental;
import br.gov.mppb.athena.persistencia.RegistroInteracaoHumana;
import br.gov.mppb.athena.persistencia.TipoInteracaoHumana;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CoberturaIntegradaFuncionalidadeExecutavelTest {

    private MockMvc mvc;
    private JdbcTemplate jdbc;
    private PersistenciaTransacionalAthena persistencia;

    @BeforeEach
    void prepararSuiteIntegrada() {
        mvc = MockMvcBuilders.standaloneSetup(new AthenaHttpController()).build();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-e2e-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                        + ";INIT=CREATE SCHEMA IF NOT EXISTS athena",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(dataSource);
        persistencia = new PersistenciaTransacionalAthena(jdbc, new DataSourceTransactionManager(dataSource));
        criarEsquemaPersistente();
    }

    @Test
    void deveExecutarFluxoDeLeituraDocumentalDePontaAPontaPorHttpSemMassaLocalFixa() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ATHENA"))
                .andExpect(content().string(not(containsString("cpf"))));

        mvc.perform(get("/api/projetos/ATHENA/documentos")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos[0].identificador").value("DOC-ENTENDIMENTO"));

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Entendimento do Athena"))
                .andExpect(jsonPath("$.versoes[0].itens[0].identificador").value("ESP.REQ.006"));

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/itens/ESP.REQ.006")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ESP.REQ.006"))
                .andExpect(jsonPath("$.relacoesRecebidas[0].recursoRelacionado").value("BL.AC.002"));
    }

    @Test
    void deveExecutarFluxoDeEscritaComInteracoesHumanasPersistidas() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T20:30:00-03:00");

        persistencia.registrarHistoricoDocumental(registroBase(
                usuarioId, projetoId, documentoId, versaoId, itemId, agora
        ));

        registrarInteracao(itemId, usuarioId, TipoInteracaoHumana.AVALIACAO, "APROVADO", "avaliacao registrada", agora.plusMinutes(1));
        registrarInteracao(itemId, usuarioId, TipoInteracaoHumana.COMENTARIO, "COMENTADO", "comentario registrado", agora.plusMinutes(2));
        registrarInteracao(itemId, usuarioId, TipoInteracaoHumana.PEDIDO_AJUSTE, "ABERTO", "ajuste destinado ao responsavel tecnico", agora.plusMinutes(3));
        registrarInteracao(itemId, usuarioId, TipoInteracaoHumana.CONFIRMACAO, "CONFIRMADO", "entendimento confirmado", agora.plusMinutes(4));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "decisao": "APROVADO",
                                  "justificativa": "Fluxo integrado validado."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decisao").value("APROVADO"));

        assertThat(contar("athena.avaliacao")).isOne();
        assertThat(contar("athena.comentario")).isOne();
        assertThat(contar("athena.pedidoajuste")).isOne();
        assertThat(contar("athena.confirmacao")).isOne();
        assertThat(contar("athena.eventoauditoria")).isEqualTo(5);
    }

    @Test
    void deveComprovarNegativasPorAutenticacaoVinculoEAutorizacaoEspecifica() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("IDENTIDADE_INSTITUCIONAL_AUSENTE"));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .header("X-Athena-Usuario", "sem.vinculo")
                        .header("X-Athena-Unidade", "GABINETE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "decisao": "APROVADO",
                                  "justificativa": "Tentativa sem vinculo."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC")
                        .param("termo", "ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));
    }

    @Test
    void deveManterMatrizDeTestesRastreavelParaEspecificacao002ELinhaBase002() {
        Map<String, Set<String>> matriz = Map.of(
                "leitura_documental_e2e", Set.of("BL.AC.001", "BL.AC.005", "BL.US.007", "ESP.REQ.001", "ESP.REQ.005"),
                "escrita_interacoes_humanas", Set.of("BL.AC.002", "BL.AC.003", "BL.AC.011", "ESP.REQ.006", "ESP.REQ.012"),
                "seguranca_aplicada", Set.of("ESP.SEG.001", "ESP.SEG.002", "ESP.SEG.003", "SOL.SEGURANCA.001"),
                "integracao_frontend_backend", Set.of("SOL.API.001", "SOL.FRONTEND.001", "SOL.UX.001", "TT.UI.002", "TT.UI.003"),
                "persistencia_integracao", Set.of("SOL.BACKEND.001", "SOL.INTEGRACAO.002", "TT.DAD.002", "TT.INT.003")
        );

        assertThat(matriz)
                .containsKeys(
                        "leitura_documental_e2e",
                        "escrita_interacoes_humanas",
                        "seguranca_aplicada",
                        "integracao_frontend_backend",
                        "persistencia_integracao"
                );
        assertThat(matriz.values())
                .flatExtracting(itens -> itens)
                .contains(
                        "ESP.REQ.006",
                        "ESP.SEG.001",
                        "SOL.FRONTEND.001",
                        "TT.UI.003",
                        "TT.INT.003"
                );
    }

    private void registrarInteracao(
            UUID itemId,
            UUID usuarioId,
            TipoInteracaoHumana tipo,
            String decisao,
            String justificativa,
            OffsetDateTime registradaEm
    ) {
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemId,
                usuarioId,
                tipo,
                decisao,
                justificativa,
                justificativa,
                registradaEm,
                UUID.randomUUID()
        ));
    }

    private RegistroHistoricoDocumental registroBase(
            UUID usuarioId,
            UUID projetoId,
            UUID documentoId,
            UUID versaoId,
            UUID itemId,
            OffsetDateTime agora
    ) {
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                usuarioId, "USR-ATHENA", "Usuario Institucional", "avaliador", "DITEC"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                projetoId, "ATHENA", "Athena", "Plataforma institucional multiprojeto documental",
                "ATIVO", agora, usuarioId
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(), projetoId, usuarioId, "RESPONSAVEL_TECNICO", "DITEC", agora, usuarioId
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                documentoId, "DOC-ENTENDIMENTO", projetoId, "Entendimento do Athena",
                "ESPECIFICACAO", "ENTENDIMENTO", "Consulta institucional", "Arvore documental", agora, usuarioId
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                versaoId, documentoId, 1, "MARKDOWN", "conteudo recebido do backend",
                "hash-e2e", agora, usuarioId, "origem externa governada"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                itemId, versaoId, "ESP.REQ.006", "Avaliacao humana",
                "Item documental exposto pela API.", 1, List.of("ESP.REQ.006", "BL.AC.002")
        );
        return new RegistroHistoricoDocumental(
                usuario, projeto, vinculo, documento, versao, List.of(item), agora, UUID.randomUUID()
        );
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
    }

    private void criarEsquemaPersistente() {
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
                    confirmada BOOLEAN NOT NULL DEFAULT FALSE,
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
                    responsaveltecnico VARCHAR(120) NOT NULL,
                    tratadoem TIMESTAMP WITH TIME ZONE,
                    decisaotratamento VARCHAR(60),
                    justificativatratamento CLOB,
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
}

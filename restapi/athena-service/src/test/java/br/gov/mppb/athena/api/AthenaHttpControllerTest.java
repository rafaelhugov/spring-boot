package br.gov.mppb.athena.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.mppb.athena.persistencia.DadosDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosItemDocumentalPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosUsuarioPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosVinculoProjetoPersistente;
import br.gov.mppb.athena.persistencia.PersistenciaTransacionalAthena;
import br.gov.mppb.athena.persistencia.RegistroHistoricoDocumental;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AthenaHttpControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new AthenaHttpController()).build();
    }

    @Test
    void deveExecutarFluxoDeLeituraDocumentalPorHttpComRespostaAutorizada() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/itens/ESP.REQ.006")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ESP.REQ.006"))
                .andExpect(jsonPath("$.rastreabilidade[0]").value("ESP.REQ.006"))
                .andExpect(jsonPath("$.relacoesRecebidas[0].tipo").value("rastreia"))
                .andExpect(content().string(not(containsString("cpf"))));
    }

    @Test
    void deveExecutarFluxoDeEscritaPorHttpComAvaliacaoAutorizada() throws Exception {
        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "decisao": "APROVADO",
                                  "justificativa": "Contrato HTTP validado."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referencia.documento").value("DOC-ENTENDIMENTO"))
                .andExpect(jsonPath("$.decisao").value("APROVADO"));
    }

    @Test
    void deveResponder401QuandoIdentidadeInstitucionalNaoForInformada() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("IDENTIDADE_INSTITUCIONAL_AUSENTE"));
    }

    @Test
    void deveResponder403QuandoUsuarioNaoTemVinculoOuAutorizacao() throws Exception {
        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .header("X-Athena-Usuario", "sem.vinculo")
                        .header("X-Athena-Unidade", "GABINETE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "decisao": "APROVADO",
                                  "justificativa": "Tentativa sem autorizacao."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));
    }

    @Test
    void deveResponder400ParaEntradaInvalida() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC")
                        .header("X-Athena-Autorizacoes", "ATHENA_ADMINISTRAR")
                        .param("termo", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void devePesquisarParticipantesCorporativosPorHttpSemExporCpf() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC")
                        .header("X-Athena-Autorizacoes", "ATHENA_ADMINISTRAR")
                        .param("termo", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantes[0].nome").value("Ana Institucional"))
                .andExpect(jsonPath("$.participantes[0].matricula").value("123456"))
                .andExpect(jsonPath("$.participantes[0].login").value("ana.institucional"))
                .andExpect(content().string(not(containsString("cpf"))))
                .andExpect(content().string(not(containsString("000.000.000-00"))));
    }

    @Test
    void deveNegarPesquisaCorporativaSemAutorizacaoAdministrativa() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .header("X-Athena-Usuario", "avaliador")
                        .header("X-Athena-Unidade", "DITEC")
                        .param("termo", "ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));
    }

    @Test
    void deveConsultarProjetoDocumentoEItemPersistidosPorHttpSemMassaEmMemoria() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-api-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                        + ";INIT=CREATE SCHEMA IF NOT EXISTS athena",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        criarEsquemaPersistente(jdbc);
        PersistenciaTransacionalAthena persistencia = new PersistenciaTransacionalAthena(
                jdbc,
                new DataSourceTransactionManager(dataSource)
        );
        registrarProjetoDocumentalPersistente(persistencia);
        MockMvc mvcPersistente = MockMvcBuilders
                .standaloneSetup(new AthenaHttpController(persistencia))
                .build();

        mvcPersistente.perform(get("/api/projetos/ATHENA")
                        .header("X-Athena-Usuario", "responsavel")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.documentos").value(1))
                .andExpect(jsonPath("$.indicadores.versoes").value(1))
                .andExpect(jsonPath("$.indicadores.participantes").value(1));

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/itens/ITEM-PERSISTIDO")
                        .header("X-Athena-Usuario", "responsavel")
                        .header("X-Athena-Unidade", "DITEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ITEM-PERSISTIDO"))
                .andExpect(jsonPath("$.conteudo").value("Conteudo persistido e consultado por HTTP."))
                .andExpect(jsonPath("$.rastreabilidade[0]").value("ESP.REQ.001"))
                .andExpect(content().string(not(containsString("DOC-ENTENDIMENTO"))));
    }

    @Test
    void devePersistirInteracoesPorHttpELerHistoricoOperacional() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-api-interacoes-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                        + ";INIT=CREATE SCHEMA IF NOT EXISTS athena",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        criarEsquemaPersistente(jdbc);
        PersistenciaTransacionalAthena persistencia = new PersistenciaTransacionalAthena(
                jdbc,
                new DataSourceTransactionManager(dataSource)
        );
        registrarProjetoDocumentalPersistente(persistencia);
        MockMvc mvcPersistente = MockMvcBuilders
                .standaloneSetup(new AthenaHttpController(persistencia))
                .build();

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/avaliacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ITEM-PERSISTIDO",
                                  "decisao": "APROVADO",
                                  "justificativa": "Avaliacao persistente."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AVALIACAO"))
                .andExpect(jsonPath("$.decisao").value("APROVADO"));

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/comentarios")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ITEM-PERSISTIDO",
                                  "texto": "Comentario persistente.",
                                  "decisaoRelacionada": "APROVADO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("COMENTARIO"))
                .andExpect(jsonPath("$.conteudo").value("Comentario persistente."));

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/ajustes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ITEM-PERSISTIDO",
                                  "justificativa": "Ajuste sem sobrescrever o documento.",
                                  "processoExternoResponsavel": "processo-documental-externo"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PEDIDO_AJUSTE"))
                .andExpect(jsonPath("$.documento").value("DOC-PERSISTIDO"));

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/confirmacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoConfirmada": 1,
                                  "escopo": "Confirmacao formal da versao persistida."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CONFIRMACAO"))
                .andExpect(jsonPath("$.decisao").value("CONFIRMADO"));

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/historico")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[0].tipo").value("HISTORICO_DOCUMENTAL_REGISTRADO"))
                .andExpect(jsonPath("$.eventos[1].tipo").value("AVALIACAO"))
                .andExpect(jsonPath("$.eventos[2].tipo").value("COMENTARIO"))
                .andExpect(jsonPath("$.eventos[3].tipo").value("PEDIDO_AJUSTE"))
                .andExpect(jsonPath("$.eventos[4].tipo").value("CONFIRMACAO"))
                .andExpect(jsonPath("$.eventos[4].usuario.login").value("responsavel"))
                .andExpect(content().string(not(containsString("cpf"))));

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versoes.length()").value(1))
                .andExpect(jsonPath("$.versoes[0].conteudoOriginal").value("Conteudo original persistido."));
    }

    private static org.springframework.http.HttpHeaders cabecalhosResponsavel() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Athena-Usuario", "responsavel");
        headers.add("X-Athena-Unidade", "DITEC");
        return headers;
    }

    private static void registrarProjetoDocumentalPersistente(PersistenciaTransacionalAthena persistencia) {
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T10:00:00-03:00");
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                usuarioId,
                "usuario-responsavel",
                "Responsavel Tecnico",
                "responsavel",
                "DITEC"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                projetoId,
                "ATHENA",
                "Athena",
                "Projeto persistido para consulta operacional.",
                "ATIVO",
                agora,
                usuarioId
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(),
                projetoId,
                usuarioId,
                "RESPONSAVEL_TECNICO",
                "DITEC",
                agora,
                usuarioId
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                documentoId,
                "DOC-PERSISTIDO",
                projetoId,
                "Documento persistido",
                "Especificacao",
                "Requisito",
                "Consulta operacional",
                "Arvore documental",
                agora,
                usuarioId
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                versaoId,
                documentoId,
                1,
                "MARKDOWN",
                "Conteudo original persistido.",
                "hash-persistido",
                agora,
                usuarioId,
                "teste integrado"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                itemId,
                versaoId,
                "ITEM-PERSISTIDO",
                "Item persistido",
                "Conteudo persistido e consultado por HTTP.",
                1,
                List.of("ESP.REQ.001", "SOL.API.001")
        );
        persistencia.registrarHistoricoDocumental(new RegistroHistoricoDocumental(
                usuario,
                projeto,
                vinculo,
                documento,
                versao,
                List.of(item),
                agora,
                UUID.randomUUID()
        ));
    }

    private static void criarEsquemaPersistente(JdbcTemplate jdbc) {
        jdbc.execute("""
                CREATE TABLE athena.usuarioinstitucional (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    login VARCHAR(120) NOT NULL UNIQUE,
                    unidade VARCHAR(80) NOT NULL
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
                    registradoporid UUID NOT NULL
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
                    criadoporid UUID NOT NULL
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
                    origemexterna VARCHAR(240)
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
                    fontesrastreabilidade VARCHAR ARRAY NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.relacaorecebida (
                    id UUID PRIMARY KEY,
                    documentoid UUID,
                    itemid UUID,
                    tipo VARCHAR(80) NOT NULL,
                    recursorelacionado VARCHAR(160) NOT NULL
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
                    solicitadoem TIMESTAMP WITH TIME ZONE NOT NULL
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

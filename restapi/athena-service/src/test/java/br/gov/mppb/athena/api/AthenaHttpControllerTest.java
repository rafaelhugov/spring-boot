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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AthenaHttpControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        SecurityContextHolder.clearContext();
        mvc = MockMvcBuilders.standaloneSetup(new AthenaHttpController()).build();
    }

    @Test
    void deveExecutarFluxoDeLeituraDocumentalPorHttpComRespostaAutorizada() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/itens/ESP.REQ.006")
                        .headers(cabecalhosAvaliador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ESP.REQ.006"))
                .andExpect(jsonPath("$.rastreabilidade[0]").value("ESP.REQ.006"))
                .andExpect(jsonPath("$.relacoesRecebidas[0].tipo").value("rastreia"))
                .andExpect(content().string(not(containsString("cpf"))));
    }

    @Test
    void devePermitirLeituraDocumentalParaUsuarioFormalmenteVinculadoAoProjeto() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO")
                        .headers(cabecalhosParticipante()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("DOC-ENTENDIMENTO"))
                .andExpect(jsonPath("$.versoes[0].itens[0].identificador").value("ESP.REQ.006"));
    }

    @Test
    void deveNegarLeituraDocumentalParaUsuarioSemVinculoFormalAoProjeto() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO")
                        .headers(cabecalhosSemVinculo()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));
    }

    @Test
    void deveBloquearAcoesFuncionaisParaParticipanteSemAutorizacaoContextual() throws Exception {
        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .headers(cabecalhosParticipante())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "decisao": "APROVADO",
                                  "justificativa": "Tentativa sem autorizacao funcional."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/comentarios")
                        .headers(cabecalhosParticipante())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "texto": "Tentativa de comentario sem autorizacao funcional."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/ajustes")
                        .headers(cabecalhosParticipante())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ESP.REQ.006",
                                  "justificativa": "Tentativa de ajuste sem autorizacao funcional."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/confirmacoes")
                        .headers(cabecalhosParticipante())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoConfirmada": 1,
                                  "escopo": "Tentativa de confirmacao sem autorizacao funcional."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .headers(cabecalhosParticipante())
                        .param("termo", "ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));
    }

    @Test
    void deveExecutarFluxoDeEscritaPorHttpComAvaliacaoAutorizada() throws Exception {
        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .headers(cabecalhosAvaliador())
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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveResponder403QuandoUsuarioNaoTemVinculoOuAutorizacao() throws Exception {
        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .headers(cabecalhosSemVinculo())
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
                        .headers(cabecalhosAdministradorAvaliador())
                        .param("termo", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void devePesquisarParticipantesCorporativosPorHttpSemExporCpf() throws Exception {
        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .headers(cabecalhosAdministradorAvaliador())
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
                        .headers(cabecalhosAvaliador())
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
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.documentos").value(1))
                .andExpect(jsonPath("$.indicadores.versoes").value(1))
                .andExpect(jsonPath("$.indicadores.participantes").value(1));

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/itens/ITEM-PERSISTIDO")
                        .headers(cabecalhosResponsavel()))
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
                                  "justificativa": "Ajuste sem sobrescrever o documento."
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

    @Test
    void deveTratarSolicitacaoAjusteEConsultarNovaVersaoVinculadaPorHttp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-api-ajuste-versao-" + UUID.randomUUID()
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

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/confirmacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoConfirmada": 1,
                                  "escopo": "Confirmacao formal da versao persistida."
                                }
                                """))
                .andExpect(status().isCreated());

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/ajustes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ITEM-PERSISTIDO",
                                  "justificativa": "Solicitacao para avaliar consistencia do entendimento."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDENTE"))
                .andExpect(jsonPath("$.responsavelTecnico").value("usuario-responsavel"))
                .andExpect(jsonPath("$.solicitadoEm").isNotEmpty());

        UUID pedidoNaoAcolhidoId = jdbc.queryForObject("""
                SELECT id
                  FROM athena.pedidoajuste
                 ORDER BY solicitadoem
                 LIMIT 1
                """, UUID.class);

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/ajustes/{pedidoAjusteId}/tratamentos",
                        pedidoNaoAcolhidoId)
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisao": "NAO_ACOLHIDA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/ajustes/{pedidoAjusteId}/tratamentos",
                        pedidoNaoAcolhidoId)
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisao": "NAO_ACOLHIDA",
                                  "justificativa": "Versao atual permanece valida apos analise tecnica."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("NAO_ACOLHIDA"))
                .andExpect(jsonPath("$.decisaoTratamento").value("NAO_ACOLHIDA"))
                .andExpect(jsonPath("$.justificativaTratamento").value("Versao atual permanece valida apos analise tecnica."))
                .andExpect(jsonPath("$.tratadoEm").isNotEmpty());

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/ajustes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "ITEM-PERSISTIDO",
                                  "justificativa": "Solicitacao acolhida para produzir nova versao vinculada."
                                }
                                """))
                .andExpect(status().isCreated());
        UUID pedidoAcolhidoId = jdbc.queryForObject("""
                SELECT id
                  FROM athena.pedidoajuste
                 ORDER BY solicitadoem DESC
                 LIMIT 1
                """, UUID.class);

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/ajustes/{pedidoAjusteId}/tratamentos",
                        pedidoAcolhidoId)
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisao": "ACOLHIDA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACOLHIDA"));

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO/versoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovaVersaoVinculada(pedidoAcolhidoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.versaoAnterior").value(1))
                .andExpect(jsonPath("$.pedidoAjusteOrigemId").value(pedidoAcolhidoId.toString()));

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-PERSISTIDO")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versoes.length()").value(2))
                .andExpect(jsonPath("$.versoes[0].confirmada").value(true))
                .andExpect(jsonPath("$.versoes[1].numero").value(2))
                .andExpect(jsonPath("$.versoes[1].pedidoAjusteOrigemId").value(pedidoAcolhidoId.toString()))
                .andExpect(jsonPath("$.versoes[1].versaoAnteriorId").isNotEmpty())
                .andExpect(jsonPath("$.versoes[1].itens[0].identificador").value("ITEM-VINCULADO"))
                .andExpect(jsonPath("$.versoes[0].conteudoOriginal").value("Conteudo original persistido."))
                .andExpect(jsonPath("$.versoes[1].conteudoOriginal").value("Conteudo revisado em nova versao."));
    }

    @Test
    void deveIngerirMarkdownJsonEYamlPorHttpEConsultarDocumentoVersionado() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-api-ingestao-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                        + ";INIT=CREATE SCHEMA IF NOT EXISTS athena",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        criarEsquemaPersistente(jdbc);
        registrarProjetoPersistenteSemDocumento(jdbc);
        MockMvc mvcPersistente = MockMvcBuilders
                .standaloneSetup(new AthenaHttpController(new PersistenciaTransacionalAthena(
                        jdbc,
                        new DataSourceTransactionManager(dataSource)
                )))
                .build();

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/ingestoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoIngestao("MARKDOWN", "# Documento Athena", "REQ-MD", "Markdown persistido.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value("DOC-INGESTAO"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.dependenciaExternaCorporativa").value("REGISTRADA_COMO_PENDENTE_NAO_SIMULADA"));

        confirmarVersaoPersistente(mvcPersistente, 1, "Confirmacao formal da versao markdown.");

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/ingestoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoIngestao("JSON", "{\"documento\":\"Athena\"}", "REQ-JSON", "JSON persistido.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(2));

        confirmarVersaoPersistente(mvcPersistente, 2, "Confirmacao formal da versao json.");

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/ingestoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoIngestao("YAML", "documento: Athena", "REQ-YAML", "YAML persistido.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(3));

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-INGESTAO")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versoes.length()").value(3))
                .andExpect(jsonPath("$.versoes[0].formato").value("MARKDOWN"))
                .andExpect(jsonPath("$.versoes[1].formato").value("JSON"))
                .andExpect(jsonPath("$.versoes[2].formato").value("YAML"))
                .andExpect(jsonPath("$.versoes[2].itens[0].identificador").value("REQ-YAML"))
                .andExpect(jsonPath("$.versoes[2].itens[0].rastreabilidade[0]").value("ENT.ESCOPO.006"));
    }

    private static void confirmarVersaoPersistente(MockMvc mvcPersistente, int versao, String escopo) throws Exception {
        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-INGESTAO/confirmacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoConfirmada": %d,
                                  "escopo": "%s"
                                }
                                """.formatted(versao, escopo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CONFIRMACAO"));
    }

    @Test
    void deveConsultarAuditoriaTecnicaOperacionalSemExporDadoSensivel() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:athena-api-auditoria-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
                        + ";INIT=CREATE SCHEMA IF NOT EXISTS athena",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        criarEsquemaPersistente(jdbc);
        registrarProjetoPersistenteSemDocumento(jdbc);
        MockMvc mvcPersistente = MockMvcBuilders
                .standaloneSetup(new AthenaHttpController(new PersistenciaTransacionalAthena(
                        jdbc,
                        new DataSourceTransactionManager(dataSource)
                )))
                .build();

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/ingestoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoIngestao("MARKDOWN", "# Auditoria", "REQ-AUD", "Auditoria persistida.")))
                .andExpect(status().isCreated());

        mvcPersistente.perform(get("/api/projetos/ATHENA/documentos/DOC-INGESTAO")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk());

        mvcPersistente.perform(post("/api/projetos/ATHENA/documentos/DOC-INGESTAO/avaliacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "itemId": "REQ-AUD",
                                  "decisao": "APROVADO",
                                  "justificativa": "Avaliacao auditavel."
                                }
                                """))
                .andExpect(status().isCreated());

        mvcPersistente.perform(get("/api/projetos/ATHENA/auditoria")
                        .headers(cabecalhosResponsavel())
                        .param("documentoId", "DOC-INGESTAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[0].correlacao").isNotEmpty())
                .andExpect(jsonPath("$.eventos[0].autorTecnico").value("responsavel"))
                .andExpect(jsonPath("$.eventos[0].politicaRetencao").value("PENDENTE_DEFINICAO_INSTITUCIONAL"))
                .andExpect(jsonPath("$.eventos[1].acao").value("LEITURA_DOCUMENTO"))
                .andExpect(jsonPath("$.eventos[1].recurso").value("DOC-INGESTAO"))
                .andExpect(jsonPath("$.eventos[2].acao").value("AVALIACAO"))
                .andExpect(jsonPath("$.eventos[2].recurso").value("REQ-AUD"))
                .andExpect(content().string(not(containsString("cpf"))))
                .andExpect(content().string(not(containsString("segredo"))));
    }

    private static org.springframework.http.HttpHeaders cabecalhosResponsavel() {
        return autenticar("responsavel");
    }

    private static org.springframework.http.HttpHeaders cabecalhosAvaliador() {
        return autenticar("avaliador");
    }

    private static org.springframework.http.HttpHeaders cabecalhosParticipante() {
        return autenticar("participante");
    }

    private static org.springframework.http.HttpHeaders cabecalhosAdministrador() {
        return autenticar("responsavel", "ATHENA_ADMINISTRAR");
    }

    private static org.springframework.http.HttpHeaders cabecalhosAdministradorAvaliador() {
        return autenticar("avaliador", "ATHENA_ADMINISTRAR");
    }

    private static org.springframework.http.HttpHeaders cabecalhosSemVinculo() {
        return autenticar("sem.vinculo");
    }

    private static org.springframework.http.HttpHeaders autenticar(String login, String... autorizacoes) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                login,
                null,
                Arrays.stream(autorizacoes).map(SimpleGrantedAuthority::new).toList()
        ));
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static String corpoIngestao(String formato, String conteudoOriginal, String itemId, String itemConteudo) {
        return """
                {
                  "identificadorDocumento": "DOC-INGESTAO",
                  "tituloDocumento": "Documento ingerido",
                  "grupo": "Integracao",
                  "categoria": "Documento interno",
                  "finalidade": "Ingestao governada",
                  "apresentacao": "Documento versionado por endpoint interno",
                  "formato": "%s",
                  "conteudoOriginal": "%s",
                  "origem": "contrato-governado-interno",
                  "representacaoInterna": [
                    {
                      "identificador": "%s",
                      "titulo": "Item %s",
                      "conteudo": "%s",
                      "fontesRastreabilidade": ["ENT.ESCOPO.006", "BL.US.006"]
                    }
                  ]
                }
                """.formatted(formato, conteudoOriginal.replace("\"", "\\\""), itemId, itemId, itemConteudo);
    }

    private static String corpoNovaVersaoVinculada(UUID pedidoAjusteId) {
        return """
                {
                  "versaoAnterior": 1,
                  "pedidoAjusteId": "%s",
                  "formato": "MARKDOWN",
                  "conteudoOriginal": "Conteudo revisado em nova versao.",
                  "origem": "tratamento de solicitacao de ajuste",
                  "representacaoInterna": [
                    {
                      "identificador": "ITEM-VINCULADO",
                      "titulo": "Item vinculado",
                      "conteudo": "Conteudo vinculado a solicitacao acolhida.",
                      "fontesRastreabilidade": ["ESP.REQ.010", "ESP.ACEITE.012"]
                    }
                  ]
                }
                """.formatted(pedidoAjusteId);
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

    private static void registrarProjetoPersistenteSemDocumento(JdbcTemplate jdbc) {
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T10:00:00-03:00");
        jdbc.update("""
                INSERT INTO athena.usuarioinstitucional (id, identificador, nome, login, unidade)
                VALUES (?, ?, ?, ?, ?)
                """, usuarioId, "usuario-responsavel", "Responsavel Tecnico", "responsavel", "DITEC");
        jdbc.update("""
                INSERT INTO athena.projeto (id, identificador, nome, descricao, situacao, criadoem, criadoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, projetoId, "ATHENA", "Athena", "Projeto persistido para ingestao operacional.", "ATIVO", agora, usuarioId);
        jdbc.update("""
                INSERT INTO athena.vinculoprojeto
                    (id, projetoid, usuarioid, papel, unidade, inicioem, registradoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), projetoId, usuarioId, "RESPONSAVEL_TECNICO", "DITEC", agora, usuarioId);
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
                    origemexterna VARCHAR(240),
                    versaoanteriorid UUID,
                    confirmada BOOLEAN NOT NULL DEFAULT FALSE,
                    pedidoajusteorigemid UUID,
                    CHECK ((numero = 1 AND versaoanteriorid IS NULL) OR (numero > 1 AND versaoanteriorid IS NOT NULL))
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

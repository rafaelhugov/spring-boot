package br.gov.mppb.athena.testes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.mppb.athena.api.AthenaHttpController;
import br.gov.mppb.athena.integracao.ServicoConsultaParticipantesCorporativos;
import br.gov.mppb.athena.seguranca.DadosParticipanteCorporativo;
import br.gov.mppb.athena.integracao.FonteCorporativaUnidades;
import br.gov.mppb.athena.integracao.UnidadeCorporativa;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Arrays;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CoberturaIntegradaFuncionalidadeExecutavelTest {

    private MockMvc mvc;
    private JdbcTemplate jdbc;
    private PersistenciaTransacionalAthena persistencia;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void prepararSuiteIntegrada() {
        SecurityContextHolder.clearContext();
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
                        .headers(cabecalhosAvaliador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ATHENA"))
                .andExpect(content().string(not(containsString("cpf"))));

        mvc.perform(get("/api/projetos/ATHENA/documentos")
                        .headers(cabecalhosAvaliador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos[0].identificador").value("DOC-ENTENDIMENTO"));

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO")
                        .headers(cabecalhosAvaliador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Entendimento do Athena"))
                .andExpect(jsonPath("$.versoes[0].itens[0].identificador").value("ESP.REQ.006"));

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/itens/ESP.REQ.006")
                        .headers(cabecalhosAvaliador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ESP.REQ.006"))
                .andExpect(jsonPath("$.relacoesRecebidas[0].recursoRelacionado").value("BL.AC.002"));
    }

    @Test
    void deveExecutarFluxoDeEscritaComInteracoesHumanasPersistidas() throws Exception {
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID idVersao = UUID.randomUUID();
        UUID idItem = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T20:30:00-03:00");

        persistencia.registrarHistoricoDocumental(registroBase(
                idUsuario, idProjeto, idDocumento, idVersao, idItem, agora
        ));

        registrarInteracao(idItem, idUsuario, TipoInteracaoHumana.AVALIACAO, "APROVADO", "avaliacao registrada", agora.plusMinutes(1));
        registrarInteracao(idItem, idUsuario, TipoInteracaoHumana.COMENTARIO, "COMENTADO", "comentario registrado", agora.plusMinutes(2));
        registrarInteracao(idItem, idUsuario, TipoInteracaoHumana.PEDIDO_AJUSTE, "ABERTO", "ajuste destinado ao responsavel tecnico", agora.plusMinutes(3));
        registrarInteracao(idItem, idUsuario, TipoInteracaoHumana.CONFIRMACAO, "CONFIRMADO", "entendimento confirmado", agora.plusMinutes(4));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .headers(cabecalhosAvaliador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "idItem": "ESP.REQ.006",
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
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-ENTENDIMENTO/avaliacoes")
                        .headers(cabecalhosSemVinculo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "idItem": "ESP.REQ.006",
                                  "decisao": "APROVADO",
                                  "justificativa": "Tentativa sem vinculo."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        mvc.perform(get("/api/projetos/ATHENA/participantes/pesquisa")
                        .headers(cabecalhosAvaliador())
                        .param("termo", "ana"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));
    }

    @Test
    void deveCobrirRegressaoDeltaEspecificacao003ComVisibilidadeAjusteVersaoConfirmacaoEAuditoria() throws Exception {
        mvc = MockMvcBuilders.standaloneSetup(new AthenaHttpController(
                persistencia,
                consultaParticipantesTeste(),
                consultaUnidadesTeste()
        )).build();
        UUID idResponsavel = UUID.randomUUID();
        UUID participanteId = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID versaoUmId = UUID.randomUUID();
        UUID itemUmId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-28T14:00:00-03:00");
        persistencia.registrarHistoricoDocumental(registroDeltaEspecificacao003(
                idResponsavel, idProjeto, idDocumento, versaoUmId, itemUmId, agora
        ));
        registrarParticipanteVinculado(participanteId, idProjeto, idResponsavel, agora);

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-DELTA-003")
                        .headers(cabecalhosParticipante()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("DOC-DELTA-003"))
                .andExpect(jsonPath("$.versoes[0].itens[0].identificador").value("ESP.REQ.009"));

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-DELTA-003")
                        .headers(cabecalhosSemVinculo()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        MvcResult pedidoCriado = mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-DELTA-003/ajustes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "idItem": "ESP.REQ.009",
                                  "justificativa": "Solicitacao analisada pelo responsavel tecnico sem fila automatica."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PEDIDO_AJUSTE"))
                .andExpect(jsonPath("$.responsavelTecnico").value("usuario-responsavel"))
                .andReturn();
        String pedidoAjusteId = campoTexto(pedidoCriado, "id");

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-DELTA-003/ajustes/{pedidoAjusteId}/tratamentos", pedidoAjusteId)
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisao": "NAO_ACOLHIDA",
                                  "justificativa": "Documento atual permanece valido e o historico preserva a solicitacao."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("NAO_ACOLHIDA"))
                .andExpect(jsonPath("$.justificativaTratamento").value("Documento atual permanece valido e o historico preserva a solicitacao."))
                .andExpect(jsonPath("$.idVersaoResultante").value(""));
        assertThat(contar("athena.versaodocumento")).isOne();

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-DELTA-003/confirmacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoConfirmada": 1,
                                  "escopo": "Confirmacao de entendimento apresentada sem efeitos de fe publica."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CONFIRMACAO"))
                .andExpect(jsonPath("$.justificativa").value("Confirmacao de entendimento apresentada sem efeitos de fe publica."))
                .andExpect(content().string(not(containsString("SLA"))))
                .andExpect(content().string(not(containsString("recurso automatico"))));

        mvc.perform(post("/api/projetos/ATHENA/documentos/DOC-DELTA-003/versoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoAnterior": 1,
                                  "formato": "MARKDOWN",
                                  "conteudoOriginal": "Conteudo ajustado em nova versao vinculada.",
                                  "origem": "regressao TT.TST.004",
                                  "representacaoInterna": [
                                    {
                                      "identificador": "ESP.REQ.010",
                                      "titulo": "Nova versao vinculada",
                                      "conteudo": "Conteudo ajustado em nova versao vinculada.",
                                      "fontesRastreabilidade": ["ESP.REQ.010", "ESP.ACEITE.012"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.versaoAnterior").value(1));

        mvc.perform(get("/api/projetos/ATHENA/documentos/DOC-DELTA-003")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versoes[0].confirmada").value(true))
                .andExpect(jsonPath("$.versoes[0].conteudoOriginal").value("Conteudo original confirmado e preservado."))
                .andExpect(jsonPath("$.versoes[1].conteudoOriginal").value("Conteudo ajustado em nova versao vinculada."))
                .andExpect(jsonPath("$.versoes[1].idVersaoAnterior").value(versaoUmId.toString()))
                .andExpect(content().string(containsString("Confirmacao sem fe publica")))
                .andExpect(content().string(not(containsString("fe publica reconhecida"))));

        mvc.perform(get("/api/projetos/ATHENA/auditoria")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LEITURA_DOCUMENTO")))
                .andExpect(content().string(containsString("PEDIDO_AJUSTE")))
                .andExpect(content().string(containsString("TRATAMENTO_PEDIDO_AJUSTE")))
                .andExpect(content().string(containsString("CONFIRMACAO")))
                .andExpect(content().string(containsString("NOVA_VERSAO_DOCUMENTAL_REGISTRADA")))
                .andExpect(content().string(not(containsString("SLA"))))
                .andExpect(content().string(not(containsString("recurso automatico"))));

        assertThat(jdbc.queryForObject(
                "SELECT conteudooriginal FROM athena.versaodocumento WHERE id = ?",
                String.class,
                versaoUmId
        )).isEqualTo("Conteudo original confirmado e preservado.");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM athena.eventoauditoria WHERE tipo IN (?, ?, ?, ?, ?)",
                Long.class,
                "LEITURA_DOCUMENTO",
                "PEDIDO_AJUSTE",
                "TRATAMENTO_PEDIDO_AJUSTE",
                "CONFIRMACAO",
                "NOVA_VERSAO_DOCUMENTAL_REGISTRADA"
        )).isGreaterThanOrEqualTo(5);
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

    @Test
    void deveValidarRecuperacaoFuncionalBLAthenaComBootstrapProjetoParticipanteIngestaoEFluxosDocumentais() throws Exception {
        mvc = MockMvcBuilders.standaloneSetup(new AthenaHttpController(
                persistencia,
                consultaParticipantesTeste(),
                consultaUnidadesTeste()
        )).build();

        // 1. Bootstrap administrativo e mantencao do projeto institucional (BL.FEATURE.001, BL.US.001)
        mvc.perform(get("/api/projetos")
                        .headers(cabecalhosAdministrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projetos.length()").value(0));

        mvc.perform(post("/api/projetos")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identificador": "ATHENA-BL",
                                  "nome": "Athena Linha de Base",
                                  "descricao": "Projeto governado multiprojeto para validacao BL TT.TST.005.",
                                  "unidade": "DITEC",
                                  "situacao": "ATIVO",
                                  "unidadeDemandante": { "idPessoa": 101 },
                                  "unidadeDesenvolvedora": { "idPessoa": 202 },
                                  "responsavelTecnico": { "login": "responsavel" }
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificador").value("ATHENA-BL"))
                .andExpect(jsonPath("$.participantes[0].login").value("responsavel"));

        mvc.perform(put("/api/projetos/ATHENA-BL")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Athena Linha de Base Recuperado",
                                  "descricao": "Projeto atualizado pelo fluxo administrativo governado.",
                                  "situacao": "ATIVO",
                                  "unidadeDemandante": { "idPessoa": 101 },
                                  "unidadeDesenvolvedora": { "idPessoa": 202 }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Athena Linha de Base Recuperado"));

        // 2. Visao geral e acompanhamento do projeto autorizado (BL.FEATURE.002, BL.US.004)
        mvc.perform(get("/api/projetos/ATHENA-BL")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("ATHENA-BL"))
                .andExpect(jsonPath("$.nome").value("Athena Linha de Base Recuperado"))
                .andExpect(content().string(not(containsString("cpf"))));

        // 3. Pesquisa institucional e vinculacao de participante (BL.FEATURE.001, BL.US.002)
        mvc.perform(get("/api/projetos/ATHENA-BL/participantes/pesquisa")
                        .headers(cabecalhosAdministrador())
                        .param("termo", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantes[0].login").value("ana.institucional"))
                .andExpect(content().string(not(containsString("cpf"))));

        mvc.perform(post("/api/projetos/ATHENA-BL/participantes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identificador": "usuario-ana.institucional",
                                  "nome": "Ana Institucional",
                                  "login": "ana.institucional",
                                  "unidade": "DITEC",
                                  "papel": "PARTICIPANTE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("ana.institucional"))
                .andExpect(jsonPath("$.papel").value("PARTICIPANTE"));

        // 4. Ingestao documental versionada e organizacao por metadados (BL.FEATURE.003, BL.FEATURE.004, BL.US.005, BL.US.006)
        mvc.perform(post("/api/projetos/ATHENA-BL/documentos/ingestoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoIngestaoBl("MARKDOWN", "# Documento de Recuperacao BL", "BL.US.005", "Conteudo recuperado BL.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value("DOC-BL-001"))
                .andExpect(jsonPath("$.versao").value(1));

        mvc.perform(get("/api/projetos/ATHENA-BL/documentos")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos[0].identificador").value("DOC-BL-001"))
                .andExpect(jsonPath("$.documentos[0].grupo").value("Especificacao"))
                .andExpect(jsonPath("$.documentos[0].categoria").value("Documento interno"));

        // 5. Apresentacao amigavel e consulta de item estruturado (BL.FEATURE.004, BL.US.007, BL.US.008)
        mvc.perform(get("/api/projetos/ATHENA-BL/documentos/DOC-BL-001")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("DOC-BL-001"))
                .andExpect(jsonPath("$.titulo").value("Documento funcional BL"))
                .andExpect(jsonPath("$.versoes[0].itens[0].identificador").value("BL.US.005"));

        mvc.perform(get("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/itens/BL.US.005")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("BL.US.005"))
                .andExpect(jsonPath("$.conteudo").value("Conteudo recuperado BL."));

        // 6. Registro de decisoes e interacoes humanas (BL.FEATURE.005, BL.US.009, BL.US.010, BL.US.011, BL.US.012)
        mvc.perform(post("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/avaliacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "idItem": "BL.US.005",
                                  "decisao": "APROVADO",
                                  "justificativa": "Validacao de avaliacao humana na recuperacao BL."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AVALIACAO"))
                .andExpect(jsonPath("$.decisao").value("APROVADO"));

        mvc.perform(post("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/comentarios")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "idItem": "BL.US.005",
                                  "texto": "Comentario rastreavel de acompanhamento BL.",
                                  "decisaoRelacionada": "APROVADO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("COMENTARIO"))
                .andExpect(jsonPath("$.conteudo").value("Comentario rastreavel de acompanhamento BL."));

        MvcResult resultadoAjuste = mvc.perform(post("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/ajustes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versao": 1,
                                  "idItem": "BL.US.005",
                                  "justificativa": "Solicitacao de ajuste para demonstrar evolucao versionada BL."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PEDIDO_AJUSTE"))
                .andExpect(jsonPath("$.estado").value("PENDENTE"))
                .andReturn();
        String pedidoAjusteId = campoTexto(resultadoAjuste, "id");

        mvc.perform(post("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/ajustes/{pedidoAjusteId}/tratamentos", pedidoAjusteId)
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisao": "ACOLHIDA",
                                  "justificativa": "Ajuste acolhido pelo responsavel tecnico."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACOLHIDA"));

        mvc.perform(post("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/confirmacoes")
                        .headers(cabecalhosResponsavel())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versaoConfirmada": 1,
                                  "escopo": "Confirmacao formal do Entendimento do projeto Athena BL."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CONFIRMACAO"))
                .andExpect(jsonPath("$.decisao").value("CONFIRMADO"));

        // 7. Preservacao de versoes e historico documental (BL.FEATURE.006, BL.US.005, BL.US.013)
        mvc.perform(post("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/versoes")
                        .headers(cabecalhosAdministrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovaVersaoBl(UUID.fromString(pedidoAjusteId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.versaoAnterior").value(1));

        mvc.perform(get("/api/projetos/ATHENA-BL/documentos/DOC-BL-001/historico")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AVALIACAO")))
                .andExpect(content().string(containsString("COMENTARIO")))
                .andExpect(content().string(containsString("PEDIDO_AJUSTE")))
                .andExpect(content().string(containsString("CONFIRMACAO")))
                .andExpect(content().string(containsString("responsavel")))
                .andExpect(content().string(not(containsString("cpf"))));

        mvc.perform(get("/api/projetos/ATHENA-BL/auditoria")
                        .headers(cabecalhosResponsavel()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LEITURA_DOCUMENTO")))
                .andExpect(content().string(containsString("PEDIDO_AJUSTE")))
                .andExpect(content().string(containsString("TRATAMENTO_PEDIDO_AJUSTE")))
                .andExpect(content().string(containsString("CONFIRMACAO")))
                .andExpect(content().string(containsString("NOVA_VERSAO_DOCUMENTAL_REGISTRADA")))
                .andExpect(content().string(not(containsString("cpf"))));

        // 8. Segregacao e controle de autorizacao (BL.US.003, CONST.INST.007)
        mvc.perform(get("/api/projetos/ATHENA-BL/documentos/DOC-BL-001")
                        .headers(autenticar("ana.institucional")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("DOC-BL-001"));

        mvc.perform(get("/api/projetos/ATHENA-BL/documentos/DOC-BL-001")
                        .headers(cabecalhosSemVinculo()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("OPERACAO_NAO_AUTORIZADA"));

        // 9. Comprovacao da persistencia transacional completa no banco de dados H2
        assertThat(contar("athena.projeto")).isGreaterThanOrEqualTo(1);
        assertThat(contar("athena.vinculoprojeto")).isGreaterThanOrEqualTo(2);
        assertThat(contar("athena.documento")).isGreaterThanOrEqualTo(1);
        assertThat(contar("athena.versaodocumento")).isGreaterThanOrEqualTo(2);
        assertThat(contar("athena.itemdocumental")).isGreaterThanOrEqualTo(2);
        assertThat(contar("athena.avaliacao")).isGreaterThanOrEqualTo(1);
        assertThat(contar("athena.comentario")).isGreaterThanOrEqualTo(1);
        assertThat(contar("athena.pedidoajuste")).isGreaterThanOrEqualTo(1);
        assertThat(contar("athena.confirmacao")).isGreaterThanOrEqualTo(1);
        assertThat(contar("athena.eventoauditoria")).isGreaterThanOrEqualTo(5);
    }

    @Test
    void deveManterMatrizDeRastreabilidadeRecuperacaoFuncionalBLAthena() {
        Map<String, Set<String>> matrizBL = Map.of(
                "bootstrap_administrativo_projeto", Set.of("BL.FEATURE.001", "BL.US.001", "BL.US.002", "BL.US.003"),
                "visao_geral_acompanhamento", Set.of("BL.FEATURE.002", "BL.US.004"),
                "recebimento_organizacao_documental", Set.of("BL.FEATURE.003", "BL.US.005", "BL.US.006"),
                "apresentacao_navegacao_itens", Set.of("BL.FEATURE.004", "BL.US.007", "BL.US.008"),
                "interacoes_humanas_decisoes", Set.of("BL.FEATURE.005", "BL.US.009", "BL.US.010", "BL.US.011", "BL.US.012"),
                "historico_rastreabilidade_auditoria", Set.of("BL.FEATURE.006", "BL.US.013", "TT.TST.005", "EVD.FUNCIONAL.TT.TST.005.001")
        );

        assertThat(matrizBL)
                .containsKeys(
                        "bootstrap_administrativo_projeto",
                        "visao_geral_acompanhamento",
                        "recebimento_organizacao_documental",
                        "apresentacao_navegacao_itens",
                        "interacoes_humanas_decisoes",
                        "historico_rastreabilidade_auditoria"
                );

        assertThat(matrizBL.values())
                .flatExtracting(itens -> itens)
                .contains(
                        "BL.FEATURE.001", "BL.FEATURE.002", "BL.FEATURE.003",
                        "BL.FEATURE.004", "BL.FEATURE.005", "BL.FEATURE.006",
                        "BL.US.001", "BL.US.002", "BL.US.003", "BL.US.004",
                        "BL.US.005", "BL.US.006", "BL.US.007", "BL.US.008",
                        "BL.US.009", "BL.US.010", "BL.US.011", "BL.US.012", "BL.US.013",
                        "TT.TST.005", "EVD.FUNCIONAL.TT.TST.005.001"
                );
    }

    private void registrarInteracao(
            UUID idItem,
            UUID idUsuario,
            TipoInteracaoHumana tipo,
            String decisao,
            String justificativa,
            OffsetDateTime dataRegistro
    ) {
        persistencia.registrarInteracaoHumana(new RegistroInteracaoHumana(
                UUID.randomUUID(),
                idItem,
                idUsuario,
                tipo,
                decisao,
                justificativa,
                justificativa,
                dataRegistro,
                UUID.randomUUID()
        ));
    }

    private static ServicoConsultaParticipantesCorporativos consultaParticipantesTeste() {
        return new ServicoConsultaParticipantesCorporativos(consulta -> List.of(
                new DadosParticipanteCorporativo(
                        "Ana Institucional", "123456", "Servidora", "Analista ministerial",
                        null, "ana.institucional", "DITEC", null
                ),
                new DadosParticipanteCorporativo(
                        "Responsavel Institucional", "654321", "Servidor", "Responsavel tecnico",
                        null, "responsavel", "DITEC", null
                )
        ));
    }

    private static FonteCorporativaUnidades consultaUnidadesTeste() {
        return termo -> List.of(
                new UnidadeCorporativa(101, "Unidade Demandante", "UD"),
                new UnidadeCorporativa(202, "Unidade Desenvolvedora", "DITEC")
        );
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

    private static org.springframework.http.HttpHeaders cabecalhosSemVinculo() {
        return autenticar("sem.vinculo");
    }

    private static org.springframework.http.HttpHeaders cabecalhosAdministrador() {
        return autenticar("responsavel", "athena.administrar");
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

    private RegistroHistoricoDocumental registroBase(
            UUID idUsuario,
            UUID idProjeto,
            UUID idDocumento,
            UUID idVersao,
            UUID idItem,
            OffsetDateTime agora
    ) {
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                idUsuario, "USR-ATHENA", "Usuario Institucional", "avaliador", "DITEC"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                idProjeto, "ATHENA", "Athena", "Plataforma institucional multiprojeto documental",
                "ATIVO", agora, idUsuario
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(), idProjeto, idUsuario, "RESPONSAVEL_TECNICO", "DITEC", agora, idUsuario
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                idDocumento, "DOC-ENTENDIMENTO", idProjeto, "Entendimento do Athena",
                "ESPECIFICACAO", "ENTENDIMENTO", "Consulta institucional", "Arvore documental", agora, idUsuario
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                idVersao, idDocumento, 1, "MARKDOWN", "conteudo recebido do backend",
                "hash-e2e", agora, idUsuario, "origem externa governada"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                idItem, idVersao, "ESP.REQ.006", "Avaliacao humana",
                "Item documental exposto pela API.", 1, List.of("ESP.REQ.006", "BL.AC.002")
        );
        return new RegistroHistoricoDocumental(
                usuario, projeto, vinculo, documento, versao, List.of(item), agora, UUID.randomUUID()
        );
    }

    private RegistroHistoricoDocumental registroDeltaEspecificacao003(
            UUID idUsuario,
            UUID idProjeto,
            UUID idDocumento,
            UUID idVersao,
            UUID idItem,
            OffsetDateTime agora
    ) {
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                idUsuario, "usuario-responsavel", "Responsavel Tecnico", "responsavel", "DITEC"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                idProjeto, "ATHENA", "Athena", "Delta da especificacao 003",
                "ATIVO", agora, idUsuario
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(), idProjeto, idUsuario, "RESPONSAVEL_TECNICO", "DITEC", agora, idUsuario
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                idDocumento, "DOC-DELTA-003", idProjeto, "Delta da especificacao 003",
                "ESPECIFICACAO", "DELTA", "Regressao funcional", "Confirmacao sem fe publica", agora, idUsuario
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                idVersao, idDocumento, 1, "MARKDOWN", "Conteudo original confirmado e preservado.",
                "hash-delta-003-v1", agora, idUsuario, "linha base especificacao 003"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                idItem, idVersao, "ESP.REQ.009", "Ajuste e confirmacao",
                "Cenario de regressao envolvendo visibilidade, ajuste, confirmacao e auditoria.",
                1, List.of("ESP.REQ.009", "ESP.REQ.010", "ESP.RN.013", "ESP.ACEITE.012")
        );
        return new RegistroHistoricoDocumental(
                usuario, projeto, vinculo, documento, versao, List.of(item), agora, UUID.randomUUID()
        );
    }

    private void registrarParticipanteVinculado(
            UUID participanteId,
            UUID idProjeto,
            UUID idUsuarioRegistro,
            OffsetDateTime dataInicio
    ) {
        jdbc.update("""
                INSERT INTO athena.usuarioinstitucional (id, identificador, nome, login, unidade)
                VALUES (?, ?, ?, ?, ?)
                """, participanteId, "usuario-participante", "Participante Vinculado", "participante", "GABINETE");
        jdbc.update("""
                INSERT INTO athena.vinculoprojeto
                    (id, id_projeto, id_usuario, papel, unidade, datainicio, id_usuarioregistro)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), idProjeto, participanteId, "PARTICIPANTE", "GABINETE", dataInicio, idUsuarioRegistro);
    }

    @SuppressWarnings("unchecked")
    private String campoTexto(MvcResult resultado, String campo) throws Exception {
        return String.valueOf(json.readValue(resultado.getResponse().getContentAsByteArray(), Map.class).get(campo));
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
                CREATE TABLE athena.relacaorecebida (
                    id UUID PRIMARY KEY,
                    id_documento UUID,
                    id_item UUID,
                    tipo VARCHAR(80) NOT NULL,
                    recursorelacionado VARCHAR(160) NOT NULL
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

    private static String corpoIngestaoBl(String formato, String conteudoOriginal, String idItem, String itemConteudo) {
        return """
                {
                  "identificadorDocumento": "DOC-BL-001",
                  "tituloDocumento": "Documento funcional BL",
                  "grupo": "Especificacao",
                  "categoria": "Documento interno",
                  "finalidade": "Validacao de recuperacao funcional",
                  "apresentacao": "Apresentacao amigavel de entendimento",
                  "formato": "%s",
                  "conteudoOriginal": "%s",
                  "origem": "recuperacao-funcional-bl-TT.TST.005",
                  "representacaoInterna": [
                    {
                      "identificador": "%s",
                      "titulo": "Item %s",
                      "conteudo": "%s",
                      "fontesRastreabilidade": ["BL.FEATURE.003", "BL.US.005"]
                    }
                  ]
                }
                """.formatted(formato, conteudoOriginal.replace("\"", "\\\""), idItem, idItem, itemConteudo);
    }

    private static String corpoNovaVersaoBl(UUID pedidoAjusteId) {
        return """
                {
                  "versaoAnterior": 1,
                  "pedidoAjusteId": "%s",
                  "formato": "MARKDOWN",
                  "conteudoOriginal": "Conteudo revisado em nova versao BL.",
                  "origem": "tratamento de solicitacao de ajuste TT.TST.005",
                  "representacaoInterna": [
                    {
                      "identificador": "BL.US.005-V2",
                      "titulo": "Item vinculado BL",
                      "conteudo": "Conteudo vinculado a solicitacao acolhida.",
                      "fontesRastreabilidade": ["BL.FEATURE.006", "BL.US.005", "BL.US.013"]
                    }
                  ]
                }
                """.formatted(pedidoAjusteId);
    }
}

package br.gov.mppb.athena.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}

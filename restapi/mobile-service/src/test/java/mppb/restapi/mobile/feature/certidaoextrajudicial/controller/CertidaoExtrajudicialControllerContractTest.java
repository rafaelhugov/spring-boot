package mppb.restapi.mobile.feature.certidaoextrajudicial.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialRequestDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialResponseDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.service.CertidaoExtrajudicialService;

class CertidaoExtrajudicialControllerContractTest {

    private FakeCertidaoExtrajudicialService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new FakeCertidaoExtrajudicialService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CertidaoExtrajudicialController(service))
                .build();
    }

    @Test
    void emitirPdfConsomeJsonProduzPdfERecebeCamposDoFlutter() throws Exception {
        mockMvc.perform(post("/api/public/certidoes-extrajudiciais/pdf")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_PDF)
                .content("""
                        {
                          "tipoDocumento": "F",
                          "documento": "12345678900",
                          "nome": "Pessoa Q3"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(new byte[] { 37, 80, 68, 70 }));

        assertNotNull(service.requestRecebido);
        assertEquals("F", service.requestRecebido.getTipoDocumento());
        assertEquals("12345678900", service.requestRecebido.getDocumento());
        assertEquals("Pessoa Q3", service.requestRecebido.getNome());
    }

    @Test
    void emitirPdfRejeitaCampoObrigatorioAusente() throws Exception {
        mockMvc.perform(post("/api/public/certidoes-extrajudiciais/pdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "tipoDocumento": "F",
                          "documento": "12345678900"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultaPorCodigoProduzJsonComCamposOpcionaisNulos() throws Exception {
        mockMvc.perform(get("/api/public/certidoes-extrajudiciais/codigo/ABC-123")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigoVerificador").value("ABC-123"))
                .andExpect(jsonPath("$.nome").value("Pessoa Q3"))
                .andExpect(jsonPath("$.status").doesNotExist());

        assertEquals("ABC-123", service.codigoConsultaRecebido);
    }

    @Test
    void baixarPdfPorCodigoProduzPdf() throws Exception {
        mockMvc.perform(get("/api/public/certidoes-extrajudiciais/codigo/ABC-123/pdf")
                .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(new byte[] { 37, 80, 68, 70 }));

        assertEquals("ABC-123", service.codigoPdfRecebido);
    }

    private static class FakeCertidaoExtrajudicialService extends CertidaoExtrajudicialService {

        private CertidaoExtrajudicialRequestDto requestRecebido;
        private String codigoConsultaRecebido;
        private String codigoPdfRecebido;

        @Override
        public ResponseEntity<byte[]> emitirPdf(CertidaoExtrajudicialRequestDto request) {
            requestRecebido = request;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certidao.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new byte[] { 37, 80, 68, 70 });
        }

        @Override
        public CertidaoExtrajudicialResponseDto consultarPorCodigo(String codigo) {
            codigoConsultaRecebido = codigo;
            CertidaoExtrajudicialResponseDto response = new CertidaoExtrajudicialResponseDto();
            response.setCodigoVerificador(codigo);
            response.setNome("Pessoa Q3");
            return response;
        }

        @Override
        public ResponseEntity<byte[]> baixarPdfPorCodigo(String codigo) {
            codigoPdfRecebido = codigo;
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new byte[] { 37, 80, 68, 70 });
        }
    }
}

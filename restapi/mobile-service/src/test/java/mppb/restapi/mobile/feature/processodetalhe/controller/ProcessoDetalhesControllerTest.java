package mppb.restapi.mobile.feature.processodetalhe.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import mppb.restapi.mobile.core.handler.RequestValidationExceptionHandler;
import mppb.restapi.mobile.feature.processodetalhe.dto.ProcessoDetalheDTO;
import mppb.restapi.mobile.feature.processodetalhe.service.ProcessoDetalhesService;

class ProcessoDetalhesControllerTest {

    private MockMvc mockMvc;
    private FakeProcessoDetalhesService service;

    @BeforeEach
    void setUp() {
        service = new FakeProcessoDetalhesService();
        ProcessoDetalhesController controller = new ProcessoDetalhesController();
        ReflectionTestUtils.setField(controller, "service", service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RequestValidationExceptionHandler())
                .build();
    }

    @Test
    void recebeCaptchaNoCorpoENaoPermiteCache() throws Exception {
        mockMvc.perform(post("/api/public/v1/processos/123/detalhes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"captchaToken\":\"token\",\"captchaValue\":\"valor\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));

        assertEquals(123L, service.idProcesso);
        assertEquals("token", service.captchaToken);
        assertEquals("valor", service.captchaValue);
    }

    @Test
    void jsonMalformadoRetorna400SemTrace() throws Exception {
        mockMvc.perform(post("/api/public/v1/processos/123/detalhes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"captchaToken\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void metodoNaoPermitidoRetorna405() throws Exception {
        mockMvc.perform(put("/api/public/v1/processos/123/detalhes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private static class FakeProcessoDetalhesService extends ProcessoDetalhesService {
        private Long idProcesso;
        private String captchaToken;
        private String captchaValue;

        @Override
        public ProcessoDetalheDTO obterDetalhesProcesso(Long idProcesso, String captchaToken, String captchaValue) {
            this.idProcesso = idProcesso;
            this.captchaToken = captchaToken;
            this.captchaValue = captchaValue;
            return new ProcessoDetalheDTO();
        }
    }
}

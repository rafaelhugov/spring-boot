
package mppb.microservices.fundoapi.feature.cobranca.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mppb.microservices.common.infrastructure.report.JasperService;
import mppb.microservices.common.test.BaseWebMvcTest;
import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import mppb.microservices.fundoapi.feature.cobranca.service.AreaService;
import mppb.microservices.fundoapi.feature.cobranca.converter.AreaConverter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AreaController.class)
class AreaResourceTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AreaService areaService;

    @Autowired
    private AreaConverter areaConverter;

    @TestConfiguration
    static class Config {
        @Bean
        public AreaService areaService() {
            return Mockito.mock(AreaService.class);
        }

        @Bean
        public AreaConverter areaConverter() {
            return Mockito.mock(AreaConverter.class);
        }

        @Bean
        public JasperService jasperService() {
            return Mockito.mock(JasperService.class); // ✅ mock do bean que está faltando
        }
    }

    @Test
    void testAreaSelectJpql() throws Exception {
        Mockito.when(areaService.findSelectJpql()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/areas/select/jpql")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testAreaSelectJpql_comRetorno() throws Exception {
        var dto = new AreaDto(1L, "Administração");

        Mockito.when(areaService.findSelectJpql())
                .thenReturn(Collections.singletonList(dto));

        String expectedJson = toJson(List.of(dto));

        mockMvc.perform(get("/api/areas/select/jpql")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }
}
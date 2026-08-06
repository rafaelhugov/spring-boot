package br.gov.mppb.ncap.feature.entidade.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sync_ctrl_test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS administrador\\\\;CREATE SCHEMA IF NOT EXISTS autorizacao\\\\;CREATE SCHEMA IF NOT EXISTS tj\\\\;CREATE SCHEMA IF NOT EXISTS ncap\\\\;SET SCHEMA_SEARCH_PATH ncap,public",
    "api.cnmp.base-url="
})
@ActiveProfiles({"test", "insecure"})
@AutoConfigureMockMvc
class EntidadeSincronizacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornarOkAoDispararSincronizacaoManualmente() throws Exception {
        mockMvc.perform(post("/entidades/sincronizar"))
                .andExpect(status().isOk());
    }
}

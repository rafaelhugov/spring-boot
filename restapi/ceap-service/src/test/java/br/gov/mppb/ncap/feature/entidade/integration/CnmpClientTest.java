package br.gov.mppb.ncap.feature.entidade.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cnmp_client_test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS administrador\\\\;CREATE SCHEMA IF NOT EXISTS autorizacao\\\\;CREATE SCHEMA IF NOT EXISTS tj\\\\;CREATE SCHEMA IF NOT EXISTS ncap\\\\;SET SCHEMA_SEARCH_PATH ncap,public",
    "api.cnmp.base-url="
})
@ActiveProfiles({"test", "insecure"})
class CnmpClientTest {

    @Autowired
    private CnmpClient cnmpClient;

    @Test
    void deveRetornarUnidadesMockadasQuandoApiNaoConfigurada() {
        List<CnmpEntidadeDto> result = cnmpClient.obterUnidadesOficiais();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getNomecnmp()).contains("Delegacia");
    }
}

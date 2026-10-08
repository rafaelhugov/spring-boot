package mppb.restapi.mobile.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ValidadorConfiguracaoSeguraTest {

    @Test
    void falhaSemPerfilExplicito() {
        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(new MockEnvironment());

        assertThrows(IllegalStateException.class, validador::validar);
    }

    @Test
    void falhaQuandoProdCombinaComInsecure() {
        MockEnvironment environment = ambienteProducaoValido();
        environment.setActiveProfiles("prod", "insecure");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertThrows(IllegalStateException.class, validador::validar);
    }

    @Test
    void falhaQuandoShowSqlAtivoEmProd() {
        MockEnvironment environment = ambienteProducaoValido()
                .withProperty("spring.jpa.show-sql", "true");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertThrows(IllegalStateException.class, validador::validar);
    }

    @Test
    void falhaQuandoCredencialDefaultPermaneceEmProd() {
        MockEnvironment environment = ambienteProducaoValido()
                .withProperty("jcr.password", "admin");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertThrows(IllegalStateException.class, validador::validar);
    }

    @Test
    void aceitaProducaoSemConfiguracaoJcr() {
        MockEnvironment environment = ambienteProducaoValido();

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertDoesNotThrow(validador::validar);
    }

    @Test
    void aceitaConfiguracaoProducaoExplicita() {
        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(ambienteProducaoValido());

        assertDoesNotThrow(validador::validar);
    }

    @Test
    void aceitaProdComHomologEnvironmentEStoreReview() {
        MockEnvironment environment = ambienteProducaoValido()
                .withProperty("mppb.environment", "homolog")
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", "segredo-de-review-exclusivo-32-chars-456")
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertDoesNotThrow(validador::validar);
    }

    @Test
    void falhaQuandoProdComProdEnvironmentEStoreReview() {
        MockEnvironment environment = ambienteProducaoValido()
                .withProperty("mppb.environment", "prod")
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", "segredo-de-review-exclusivo-32-chars-456")
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertThrows(IllegalStateException.class, validador::validar);
    }

    @Test
    void falhaQuandoProdComStoreReviewSemHomologEnvironment() {
        MockEnvironment environment = ambienteProducaoValido()
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", "segredo-de-review-exclusivo-32-chars-456")
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(environment);

        assertThrows(IllegalStateException.class, validador::validar);
    }

    private MockEnvironment ambienteProducaoValido() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("api.auth.url", "https://auth-api.mppb.mp.br")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db-producao:5432/mobile")
                .withProperty("spring.datasource.username", "mobile_app")
                .withProperty("spring.datasource.password", "senha-forte")
                .withProperty("spring.jpa.show-sql", "false")
                .withProperty("spring.jpa.properties.hibernate.format_sql", "false")
                .withProperty("mpvirtual.recursos-humanos.base-url", "http://mpvirtual-interno/api")
                .withProperty("jcr.user", "mobile_jcr")
                .withProperty("jcr.password", "senha-jcr")
                .withProperty("cors.enabled", "false")
                .withProperty("cors.access-control-headers.allow-credentials", "false")
                .withProperty("cors.access-control-headers.allow-origin", "*");
    }
}

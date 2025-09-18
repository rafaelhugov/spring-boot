package mppb.microservices.fundoapi.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    private static final String API_TITLE = "Projeto fundo-api";
    private static final String API_DESCRIPTION = "Documentação da API do Projeto fundo-api";
    private static final String API_VERSION = "1.0.0";

    @Value("${aplicacao.openapi.url}")
    private String apiUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.info(new Info().title(API_TITLE).description(API_DESCRIPTION).version(API_VERSION));
        openAPI.addServersItem(new Server().url(apiUrl).description("URL do serviço."));
        return openAPI;
    }
}

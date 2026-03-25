package mppb.restapi.common.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Value("${project.name}")
    private String projectName;

    @Value("${project.version}")
    private String projectVersion;

    @Value("${project.description}")
    private String projectDescription;

    @Value("${project.openapi.url}")
    private String apiUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.info(new Info().title(projectName).description(projectDescription).version(projectVersion));
        openAPI.addServersItem(new Server().url(apiUrl).description("URL do serviço."));
        return openAPI;
    }
}

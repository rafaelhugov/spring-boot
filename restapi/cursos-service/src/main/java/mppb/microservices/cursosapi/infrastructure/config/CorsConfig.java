package mppb.microservices.cursosapi.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${cors.access-control-headers.allow-origin:*}")
    private String[] allowedOrigins;

    @Value("${cors.access-control-headers.allow-credentials:false}")
    private boolean allowCredentials;

    @Value("${cors.enabled:false}")
    private boolean corsEnabled;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (!corsEnabled) return;

                // Se quiser wildcard com padrões, prefira allowedOriginPatterns
                registry.addMapping("/**")
                        // Se você usa subdomínios/wildcards, troque para allowedOriginPatterns(allowedOrigins)
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET","POST","PATCH","PUT","DELETE","OPTIONS","HEAD","TRACE","CONNECT")
                        .allowedHeaders("*")
                        .allowCredentials(allowCredentials);
            }
        };
    }
}
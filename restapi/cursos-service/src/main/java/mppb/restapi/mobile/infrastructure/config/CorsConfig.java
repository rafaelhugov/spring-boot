package mppb.restapi.mobile.infrastructure.config;

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

    //private final CacheControlInterceptor cacheControlInterceptor;

    //public CorsConfig(CacheControlInterceptor cacheControlInterceptor) {
    //    this.cacheControlInterceptor = cacheControlInterceptor;
    //}

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (!corsEnabled) return;
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT")
                        .allowedHeaders("*")
                        .allowCredentials(allowCredentials);
            }

          /*  @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // Registra o interceptor de cache e o aplica apenas às rotas de lookup
                registry.addInterceptor(cacheControlInterceptor)
                        .addPathPatterns(Constants.API_PREFIX  + "/lookup/**");
            }*/
        };
    }
}
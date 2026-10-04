package mppb.restapi.mobile.feature.synthetic.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import mppb.restapi.common.infrastructure.util.Constants;

/**
 * Cadeia de segurança específica do mobile-service para permitir que /acesso/autoriza
 * seja acessível sem token, eliminando a necessidade de alterar a infraestrutura
 * compartilhada de restapi-common.
 */
@Configuration
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
public class SyntheticSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain syntheticSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/acesso/autoriza", "/" + Constants.API_PREFIX + "/acesso/autoriza")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

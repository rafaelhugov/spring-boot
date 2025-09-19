package mppb.microservices.fundoapi.infrastructure.config;


import mppb.microservices.common.infrastructure.env.RuntimeEnv;
import mppb.microservices.common.infrastructure.security.filter.CustomBasicAuthenticationFilter;
import mppb.microservices.common.infrastructure.security.filter.CustomUsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final String PUBLIC_URLS[] = {"/favicon.ico",
            "/public/**",  "/swagger-ui/index.html", "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs",
    };

    @Autowired
    private RuntimeEnv runtimeEnv;

    @Autowired
    private CustomBasicAuthenticationFilter customBasicAuthenticationFilter;

    @Autowired
    private CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        if(!runtimeEnv.shouldRequireAuthToken()){
            return http.httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable()).build();
        }

        return http.httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(PUBLIC_URLS).permitAll();
                    req.anyRequest().authenticated();
                }).addFilterBefore(customBasicAuthenticationFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(customUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
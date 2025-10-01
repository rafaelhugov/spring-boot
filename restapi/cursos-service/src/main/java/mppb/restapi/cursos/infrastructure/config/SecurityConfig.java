package mppb.restapi.cursos.infrastructure.config;


import lombok.RequiredArgsConstructor;
import mppb.restapi.common.infrastructure.env.RuntimeEnv;
import mppb.restapi.common.infrastructure.security.filter.CustomBasicAuthenticationFilter;
import mppb.restapi.common.infrastructure.security.filter.CustomUsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final String PUBLIC_URLS[] = {"/favicon.ico",
            "/public/**",  "/swagger-ui/index.html", "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs",
    };

    private final RuntimeEnv runtimeEnv;

    private final CustomBasicAuthenticationFilter customBasicAuthenticationFilter;

    private final CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        if(!runtimeEnv.shouldRequireAuthToken()){
            return http.httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
                    .headers(headers -> headers.cacheControl(cache -> cache.disable()))
                    .build();
        }



        return http.cors(Customizer.withDefaults()).httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(PUBLIC_URLS).permitAll();
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    req.anyRequest().authenticated();
                }).addFilterBefore(customBasicAuthenticationFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(customUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
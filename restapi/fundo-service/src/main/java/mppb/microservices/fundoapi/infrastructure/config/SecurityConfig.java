package mppb.microservices.fundoapi.infrastructure.config;


import mppb.microservices.common.infrastructure.env.RuntimeEnv;
import mppb.microservices.common.infrastructure.security.filter.CustomBasicAuthenticationFilter;
import mppb.microservices.common.infrastructure.security.filter.CustomUsernamePasswordAuthenticationFilter;
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
public class SecurityConfig {

    private final String PUBLIC_URLS[] = {"/favicon.ico",
            "/public/**",  "/swagger-ui/index.html", "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/api/boletos/**"
    };

    @Autowired
    private RuntimeEnv runtimeEnv;

    @Autowired
    private CustomBasicAuthenticationFilter customBasicAuthenticationFilter;

    @Autowired
    private CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());

        if (!runtimeEnv.shouldRequireAuthToken()) {
            return http
                    .httpBasic(Customizer.withDefaults())
                    .authorizeHttpRequests(req -> req
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .anyRequest().permitAll()
                    )
                    .build();
        }

        return http
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    req.requestMatchers(PUBLIC_URLS).permitAll();
                    req.anyRequest().authenticated();
                })
                // Add filters AFTER CorsFilter has processed preflight headers
                .addFilterBefore(customBasicAuthenticationFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(customUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
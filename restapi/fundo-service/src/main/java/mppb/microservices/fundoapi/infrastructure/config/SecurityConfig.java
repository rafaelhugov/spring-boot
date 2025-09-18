package mppb.microservices.fundoapi.infrastructure.config;


import mppb.microservices.common.infrastructure.security.filter.CustomBasicAuthenticationFilter;
import mppb.microservices.common.infrastructure.security.filter.CustomUsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private final String PUBLIC_URLS[] = {"/favicon.ico",
//            "/actuator/**",
//            "**/swagger-resources/**", "**/swagger-ui/**",
            "/public/**",  "/swagger-ui/index.html", "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs",
//            "/webjars/**"
    };

    private CustomBasicAuthenticationFilter customBasicAuthenticationFilter;

    private CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter;

    public SecurityConfig(CustomBasicAuthenticationFilter securityFilter,
                          CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter) {
        this.customBasicAuthenticationFilter = securityFilter;
        this.customUsernamePasswordAuthenticationFilter = customUsernamePasswordAuthenticationFilter;
    }

    public boolean isSecureProfileActive() {
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return false;
        }
        return Arrays.asList(activeProfiles.split(",")).contains("secure");
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowCredentials(true);
//        config.addAllowedOriginPattern("*"); // or use specific origin in production
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        if(!isSecureProfileActive()){
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
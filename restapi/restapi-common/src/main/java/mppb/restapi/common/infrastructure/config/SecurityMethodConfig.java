package mppb.restapi.common.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity // This is the engine that processes @PreAuthorize
@Profile("!insecure") // This turns the engine OFF if the 'insecure' profile is active
public class SecurityMethodConfig {
}
package mppb.microservices.fundoapi.infrastructure.config;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.LocaleResolver;
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.of("pt", "BR"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Recife"));
        Locale.setDefault(Locale.of("pt", "BR"));
    }

}
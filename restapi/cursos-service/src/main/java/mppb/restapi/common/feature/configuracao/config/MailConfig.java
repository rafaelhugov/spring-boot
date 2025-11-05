package mppb.restapi.common.feature.configuracao.config;

import lombok.RequiredArgsConstructor;
import mppb.restapi.common.feature.configuracao.service.ConfiguracaoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class MailConfig {

    private final ConfiguracaoService configuracaoService;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(configuracaoService.get("global.instituicao.ip_email"));
        mailSender.setPort(configuracaoService.getAsInteger("global.instituicao.porta_email"));

        // mailSender.setUsername(configuracaoService.get("global.instituicao.email_username"));
        // mailSender.setPassword(configuracaoService.get("global.instituicao.email_password"));

        Properties props = mailSender.getJavaMailProperties();
        // props.put("mail.smtp.auth", "true");
        // props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}

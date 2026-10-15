package mppb.restapi.cursos.feature.common.service;

import mppb.restapi.common.feature.autorizacao.service.SistemaService;
import mppb.restapi.common.feature.configuracao.service.ConfiguracaoService;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmailServiceCriteriosPublicoAlvoTest {

    @Test
    void renderizaCriteriosComHtmlEscapadoEQuebrasDeLinhaSeguras() throws Exception {
        EmailService emailService = novoEmailService();
        Method formatar = EmailService.class.getDeclaredMethod("formatarCriteriosEspecificos", String.class);
        formatar.setAccessible(true);

        String html = (String) formatar.invoke(
                emailService,
                "<script>alert(\"x\")</script>\nLinha & detalhe");

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;"));
        assertTrue(html.contains("<br>Linha &amp; detalhe"));
    }

    @Test
    void omiteBlocoDeCriteriosQuandoValorForNuloOuVazio() throws Exception {
        EmailService emailService = novoEmailService();
        Method formatar = EmailService.class.getDeclaredMethod("formatarCriteriosEspecificos", String.class);
        formatar.setAccessible(true);

        assertEquals("", formatar.invoke(emailService, new Object[]{null}));
        assertEquals("", formatar.invoke(emailService, "   "));
    }

    private EmailService novoEmailService() {
        return new EmailService(
                mock(JavaMailSender.class),
                mock(ConfiguracaoService.class),
                mock(SistemaService.class),
                mock(ParticipanteService.class));
    }
}

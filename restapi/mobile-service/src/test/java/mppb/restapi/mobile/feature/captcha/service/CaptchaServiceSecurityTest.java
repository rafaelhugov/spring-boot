package mppb.restapi.mobile.feature.captcha.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CaptchaServiceSecurityTest {

    @Test
    void armazenamentoPendentePermaneceLimitado() {
        CaptchaService service = new CaptchaService();
        ReflectionTestUtils.setField(service, "maxPendingCaptchas", 3);

        for (int i = 0; i < 10; i++) {
            service.generateCaptcha();
        }

        Map<?, ?> store = (Map<?, ?>) ReflectionTestUtils.getField(service, "captchaStore");
        assertTrue(store.size() <= 3);
    }

    @Test
    void tokenContinuaSendoDeUsoUnicoMesmoComRespostaErrada() {
        CaptchaService service = new CaptchaService();
        var captcha = service.generateCaptcha();

        assertFalse(service.validateCaptcha(captcha.getToken(), "resposta-errada"));
        assertFalse(service.validateCaptcha(captcha.getToken(), "resposta-errada"));
    }
}

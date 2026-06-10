package mppb.restapi.mobile.feature.processoconsulta.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaValidationService {

    // Configurado no application.yml (ou usa um valor default/mock para desenvolvimento)
    @Value("${google.recaptcha.secret:dummy-secret}")
    private String recaptchaSecret;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify?secret={secret}&response={response}";

    public boolean validateCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            return false;
        }
        
        // Se estivermos em ambiente de dev/teste sem secret configurada, podemos mockar
        if ("dummy-secret".equals(recaptchaSecret) && "DEV_TOKEN".equals(captchaToken)) {
            return true;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    RECAPTCHA_VERIFY_URL,
                    null,
                    Map.class,
                    recaptchaSecret,
                    captchaToken
            );

            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                return true;
            }
        } catch (Exception e) {
            // Em caso de erro na API do Google, logamos e bloqueamos por segurança
            System.err.println("Erro ao validar CAPTCHA: " + e.getMessage());
        }
        
        return false;
    }
}

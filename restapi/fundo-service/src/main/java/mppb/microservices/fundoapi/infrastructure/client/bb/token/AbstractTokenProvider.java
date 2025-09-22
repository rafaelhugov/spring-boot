package mppb.microservices.fundoapi.infrastructure.client.bb.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public abstract class AbstractTokenProvider {

    private String tokenUrl;

    private String clientId;

    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedToken;
    private Instant tokenExpiration = Instant.MIN;
    private final long EXPIRE_TIME_SECONDS = 60;

    public synchronized String getToken() {
        // Verifica se o token está ausente ou prestes a expirar
        if (cachedToken == null || Instant.now().isAfter(tokenExpiration.minusSeconds(EXPIRE_TIME_SECONDS))) {
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            Map<String, Object> bodyMap = response.getBody();

            // Extrai o token de acesso e sua validade
            cachedToken = (String) bodyMap.get("access_token");
            int expiresIn = (Integer) bodyMap.get("expires_in"); // 10 minutos segundo documentação do BB
            tokenExpiration = Instant.now().plusSeconds(expiresIn);
        }

        return cachedToken;
    }
}

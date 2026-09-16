package mppb.restapi.mobile.feature.certidaoextrajudicial.service;

import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialRequestDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CertidaoExtrajudicialService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${certidao.extrajudicial.base-url:https://certservapp.mppb.mp.br}")
    private String baseUrl;

    @Value("${certidao.extrajudicial.fallback-base-url:https://certservapp.mppb.mp.br}")
    private String fallbackBaseUrl;

    public ResponseEntity<byte[]> emitirPdf(CertidaoExtrajudicialRequestDto request) {
        String url = buildUrl("/certidoes-extrajudiciais/pdf");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CertidaoExtrajudicialRequestDto> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForEntity(url, entity, byte[].class);
    }

    public CertidaoExtrajudicialResponseDto consultarPorCodigo(String codigo) {
        String path = "/certidoes-extrajudiciais/codigo/{codigo}";
        try {
            return restTemplate.getForObject(buildUrl(baseUrl, path), CertidaoExtrajudicialResponseDto.class, codigo);
        } catch (RestClientResponseException exception) {
            if (deveTentarFallback(exception)) {
                return restTemplate.getForObject(buildUrl(fallbackBaseUrl, path), CertidaoExtrajudicialResponseDto.class, codigo);
            }
            throw exception;
        }
    }

    public ResponseEntity<byte[]> baixarPdfPorCodigo(String codigo) {
        String path = "/certidoes-extrajudiciais/codigo/{codigo}/pdf";
        try {
            return restTemplate.getForEntity(buildUrl(baseUrl, path), byte[].class, codigo);
        } catch (RestClientResponseException exception) {
            if (deveTentarFallback(exception)) {
                return restTemplate.getForEntity(buildUrl(fallbackBaseUrl, path), byte[].class, codigo);
            }
            throw exception;
        }
    }

    private String buildUrl(String path) {
        return buildUrl(baseUrl, path);
    }

    private String buildUrl(String serviceBaseUrl, String path) {
        return UriComponentsBuilder.fromHttpUrl(serviceBaseUrl)
                .path(path)
                .toUriString();
    }

    private boolean deveTentarFallback(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 404
                && fallbackBaseUrl != null
                && !fallbackBaseUrl.isBlank()
                && !fallbackBaseUrl.equals(baseUrl);
    }
}

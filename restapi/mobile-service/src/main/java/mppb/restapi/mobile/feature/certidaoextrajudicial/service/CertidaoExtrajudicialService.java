package mppb.restapi.mobile.feature.certidaoextrajudicial.service;

import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialRequestDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CertidaoExtrajudicialService.class);

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
                log.warn("Consulta de certidao por codigo falhou na base principal: status={}, baseUrl={}, fallbackBaseUrl={}",
                        exception.getStatusCode().value(), baseUrl, fallbackBaseUrl);
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
                log.warn("Download de certidao por codigo falhou na base principal: status={}, baseUrl={}, fallbackBaseUrl={}",
                        exception.getStatusCode().value(), baseUrl, fallbackBaseUrl);
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
        int status = exception.getStatusCode().value();
        return status >= 400
                && fallbackBaseUrl != null
                && !fallbackBaseUrl.isBlank();
    }
}

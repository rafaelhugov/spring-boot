package mppb.restapi.mobile.feature.certidaoextrajudicial.service;

import java.net.URI;
import java.util.List;

import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialRequestDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
        headers.setAccept(List.of(MediaType.APPLICATION_PDF));
        headers.set(HttpHeaders.USER_AGENT, "MPPB-Mobile-Service");
        HttpEntity<CertidaoExtrajudicialRequestDto> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForEntity(url, entity, byte[].class);
    }

    public CertidaoExtrajudicialResponseDto consultarPorCodigo(String codigo) {
        URI uri = buildUri(baseUrl, "certidoes-extrajudiciais", "codigo", codigo);
        try {
            return restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    jsonRequestEntity(),
                    CertidaoExtrajudicialResponseDto.class
            ).getBody();
        } catch (RestClientResponseException exception) {
            if (deveTentarFallback(exception)) {
                URI fallbackUri = buildUri(fallbackBaseUrl, "certidoes-extrajudiciais", "codigo", codigo);
                log.warn("Consulta de certidao por codigo falhou na base principal: status={}, uri={}, fallbackUri={}",
                        exception.getStatusCode().value(), safeUri(uri), safeUri(fallbackUri));
                return restTemplate.exchange(
                        fallbackUri,
                        HttpMethod.GET,
                        jsonRequestEntity(),
                        CertidaoExtrajudicialResponseDto.class
                ).getBody();
            }
            throw exception;
        }
    }

    public ResponseEntity<byte[]> baixarPdfPorCodigo(String codigo) {
        URI uri = buildUri(baseUrl, "certidoes-extrajudiciais", "codigo", codigo, "pdf");
        try {
            return restTemplate.exchange(uri, HttpMethod.GET, pdfRequestEntity(), byte[].class);
        } catch (RestClientResponseException exception) {
            if (deveTentarFallback(exception)) {
                URI fallbackUri = buildUri(fallbackBaseUrl, "certidoes-extrajudiciais", "codigo", codigo, "pdf");
                log.warn("Download de certidao por codigo falhou na base principal: status={}, uri={}, fallbackUri={}",
                        exception.getStatusCode().value(), safeUri(uri), safeUri(fallbackUri));
                return restTemplate.exchange(fallbackUri, HttpMethod.GET, pdfRequestEntity(), byte[].class);
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

    private URI buildUri(String serviceBaseUrl, String... pathSegments) {
        return UriComponentsBuilder.fromHttpUrl(serviceBaseUrl)
                .pathSegment(pathSegments)
                .build()
                .toUri();
    }

    private HttpEntity<Void> jsonRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.USER_AGENT, "MPPB-Mobile-Service");
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Void> pdfRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_PDF));
        headers.set(HttpHeaders.USER_AGENT, "MPPB-Mobile-Service");
        return new HttpEntity<>(headers);
    }

    private String safeUri(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .replacePath("/certidoes-extrajudiciais/codigo/**")
                .replaceQuery(null)
                .toUriString();
    }

    private boolean deveTentarFallback(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status >= 400
                && fallbackBaseUrl != null
                && !fallbackBaseUrl.isBlank();
    }
}

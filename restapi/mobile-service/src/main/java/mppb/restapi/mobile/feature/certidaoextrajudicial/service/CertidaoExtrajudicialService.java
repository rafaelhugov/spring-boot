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
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CertidaoExtrajudicialService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${certidao.extrajudicial.base-url:https://certservapp.mppb.mp.br}")
    private String baseUrl;

    public ResponseEntity<byte[]> emitirPdf(CertidaoExtrajudicialRequestDto request) {
        String url = buildUrl("/certidoes-extrajudiciais/pdf");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CertidaoExtrajudicialRequestDto> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForEntity(url, entity, byte[].class);
    }

    public CertidaoExtrajudicialResponseDto consultarPorCodigo(String codigo) {
        String url = buildUrl("/certidoes-extrajudiciais/codigo/{codigo}");
        return restTemplate.getForObject(url, CertidaoExtrajudicialResponseDto.class, codigo);
    }

    public ResponseEntity<byte[]> baixarPdfPorCodigo(String codigo) {
        String url = buildUrl("/certidoes-extrajudiciais/codigo/{codigo}/pdf");
        return restTemplate.getForEntity(url, byte[].class, codigo);
    }

    private String buildUrl(String path) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .toUriString();
    }
}

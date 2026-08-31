package mppb.restapi.mobile.feature.concessaofolga.service;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;

@Service
public class MpVirtualConcessaoFolgaClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mpvirtual.recursos-humanos.base-url:http://10.128.4.50:8080}")
    private String baseUrl;

    @Value("${mpvirtual.recursos-humanos.orgao-id:1464408}")
    private String orgaoId;

    public ResponseEntity<String> sugestoesDatasFinais(Long concessaoId, String dataInicial, String token) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/recursoshumanos/concessoes/{concessaoId}/sugestoes-datas-finais")
                .queryParam("dataInicial", dataInicial)
                .buildAndExpand(concessaoId)
                .toUri();

        return restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers(token)),
                String.class);
    }

    public ResponseEntity<String> cadastrarSolicitacao(
            Long concessaoId,
            SolicitacaoMarcacaoFeriasRequestDto request,
            String token) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/recursoshumanos/concessoes/{concessaoId}/solicitacoes")
                .buildAndExpand(concessaoId)
                .toUri();

        return restTemplate.exchange(
                uri,
                HttpMethod.POST,
                new HttpEntity<>(request, headers(token)),
                String.class);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Orgao-Id", orgaoId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}

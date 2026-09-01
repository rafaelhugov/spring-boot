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

import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;

@Service
public class MpVirtualConcessaoFolgaClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mpvirtual.recursos-humanos.base-url:http://10.94.0.253:8080/api}")
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

    public ResponseEntity<String> consultarSolicitacao(Long solicitacaoId, String token) {
        return exchangeSolicitacao(solicitacaoId, null, HttpMethod.GET, null, token);
    }

    public ResponseEntity<String> consultarOperacoesSolicitacao(Long solicitacaoId, String token) {
        return exchangeSolicitacao(solicitacaoId, "operacoes", HttpMethod.GET, null, token);
    }

    public ResponseEntity<String> cancelarSolicitacao(Long solicitacaoId, String token) {
        return exchangeSolicitacao(solicitacaoId, "cancelamentos", HttpMethod.POST, null, token);
    }

    public ResponseEntity<String> adiarSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        return exchangeSolicitacao(solicitacaoId, "adiamentos", HttpMethod.POST, request, token);
    }

    public ResponseEntity<String> anteciparSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        return exchangeSolicitacao(solicitacaoId, "antecipacoes", HttpMethod.POST, request, token);
    }

    public ResponseEntity<String> interromperSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        return exchangeSolicitacao(solicitacaoId, "interrupcoes", HttpMethod.POST, request, token);
    }

    public ResponseEntity<String> suspenderIntegralmenteSolicitacao(Long solicitacaoId, String token) {
        return exchangeSolicitacao(solicitacaoId, "suspensoes-integrais", HttpMethod.POST, null, token);
    }

    public ResponseEntity<String> suspenderParcialmenteSolicitacao(
            Long solicitacaoId,
            OperacaoSolicitacaoRequestDto request,
            String token) {
        return exchangeSolicitacao(solicitacaoId, "suspensoes-parciais", HttpMethod.POST, request, token);
    }

    private ResponseEntity<String> exchangeSolicitacao(
            Long solicitacaoId,
            String action,
            HttpMethod method,
            Object body,
            String token) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/recursoshumanos/solicitacoes/{solicitacaoId}");
        if (action != null && !action.isBlank()) {
            builder.path("/{action}");
        }

        URI uri = action == null || action.isBlank()
                ? builder.buildAndExpand(solicitacaoId).toUri()
                : builder.buildAndExpand(solicitacaoId, action).toUri();

        return restTemplate.exchange(
                uri,
                method,
                new HttpEntity<>(body, headers(token)),
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

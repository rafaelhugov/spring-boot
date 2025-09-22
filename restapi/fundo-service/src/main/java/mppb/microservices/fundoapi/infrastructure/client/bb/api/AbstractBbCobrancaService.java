package mppb.microservices.fundoapi.infrastructure.client.bb.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import mppb.microservices.common.core.error.AppException;
import mppb.microservices.common.core.error.feign.ClientApiException;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.request.BbAlterarValorNominalRequestDto;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.request.BbDataVencimentoRequestDto;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.request.BbBoletoRequestDto;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.response.BbAlteracaoBoletoResponseDto;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.response.BbBoletoResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class AbstractBbCobrancaService {

    protected final RestTemplate restTemplate = new RestTemplate();

    @Value("${microsservicos.bb.cobranca.url}")
    private String cobrancaUrl;

    protected abstract String getToken();
    protected abstract String getClientId();
    protected abstract String getDevAppKey();

    public BbBoletoResponseDto gerarBoleto(BbBoletoRequestDto request) {

        try {

            String token = getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("X-Application-Key", getClientId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<BbBoletoRequestDto> entity = new HttpEntity<>(request, headers);
            String url = cobrancaUrl + "/cobrancas/v2/boletos?gw-dev-app-key=" + getDevAppKey();

            ResponseEntity<BbBoletoResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    BbBoletoResponseDto.class
            );

            if (response.getStatusCode().value() >= 200 && response.getStatusCode().value() < 300) {
                return response.getBody();
            }

            throw new ClientApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), new ObjectMapper().writeValueAsString(response.getBody()));

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new ClientApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), responseBody);
        }catch(AppException e) {
            throw e;
        }catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    //Utilizado HttpClient pois tem suporte nativo a HTTP Patch. RestTemplate ou WebCliente não possuem suporte nativo.
    protected <T, R> R alterarBoleto(String nossoNumero, T requestPayload, Class<R> responseType)  {
        String token = getToken();
        String url = cobrancaUrl + "/cobrancas/v2/boletos/" + nossoNumero + "?gw-dev-app-key=" + getDevAppKey();

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(requestPayload)))
                    .header("Authorization", "Bearer " + token)
                    .header("X-Application-Key", getClientId())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Verifica se a resposta foi bem-sucedida (código 2xx)
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ObjectMapper().readValue(response.body(), responseType);
            }

            // Outros casos inesperados
            throw new ClientApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Erro inesperado ao consultar API: " + response.body());
//            throw new RuntimeException("Erro inesperado ao consultar API: " + response.statusCode() + ". body: " + response.body());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ClientApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Erro HTTP ao consultar API do BB: " + e.getStatusCode());
//            throw new RuntimeException("Erro HTTP ao consultar API do BB: " + e.getStatusCode(), e);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public BbAlteracaoBoletoResponseDto alterarDataVencimento(String nossoNumero, Long numeroConvenio,
                                                             LocalDate novaDataVencimento) {
        if (novaDataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento não pode ser nula");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dataFormatada = novaDataVencimento.format(formatter);

        BbDataVencimentoRequestDto.AlteracaoData alteracaoData = new BbDataVencimentoRequestDto.AlteracaoData();
        alteracaoData.setNovaDataVencimento(dataFormatada);

        BbDataVencimentoRequestDto request = new BbDataVencimentoRequestDto();
        request.setNumeroConvenio(numeroConvenio);
        request.setIndicadorNovaDataVencimento("S");
        request.setAlteracaoData(alteracaoData);

        return alterarBoleto(nossoNumero, request, BbAlteracaoBoletoResponseDto.class);
    }

    public BbAlteracaoBoletoResponseDto alterarValorNominal(String nossoNumero, Long numeroConvenio,
                                                            Double novoValorNominal) {
        
        BbAlterarValorNominalRequestDto.AlteracaoValor valorAlterado = new BbAlterarValorNominalRequestDto.AlteracaoValor();
        valorAlterado.setNovoValorNominal(novoValorNominal);

        BbAlterarValorNominalRequestDto request = new BbAlterarValorNominalRequestDto();
        request.setNumeroConvenio(numeroConvenio);
        request.setIndicadorNovoValorNominal("S");
        request.setAlteracaoValor(valorAlterado);

        return alterarBoleto(nossoNumero, request, BbAlteracaoBoletoResponseDto.class);
    }
}

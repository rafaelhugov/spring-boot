package mppb.microservices.fundoapi.infrastructure.client.bc.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import mppb.microservices.common.core.error.ValidationException;
import mppb.microservices.fundoapi.infrastructure.client.bc.dto.BcValorSelicDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BcSelicService {

    private DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${microsservicos.bc.selic.url}")
    private String selicUrl;


    public List<BcValorSelicDto> buscarSelicDiaria(LocalDate dataInicial, LocalDate dataFinal) {

        if(dataInicial == null || dataFinal == null){
            throw new ValidationException("É necessário haver data inicial e final para buscar SELIC diária.");
        }

        String dataInicialStr = dataInicial.format(DATE_FORMATTER);
        String dataFinalStr = dataFinal.format(DATE_FORMATTER);

        String url = String.format("%s?formato=json&dataInicial=%s&dataFinal=%s",
                selicUrl, dataInicialStr, dataFinalStr);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), new TypeReference<List<BcValorSelicDto>>() {});
            }

            // Trata explicitamente o 404 retornando lista vazia
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new ArrayList<>();
            }

            // Outros casos inesperados
            throw new RuntimeException("Erro inesperado ao consultar API do BCB: " + response.getStatusCode());

        } catch (HttpClientErrorException.NotFound e) {
            // Garante que 404 caia aqui também (caso não tratado no response)
            return new ArrayList<>();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Erro HTTP ao consultar API do BCB: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar resposta da API do BCB", e);
        }
    }
}

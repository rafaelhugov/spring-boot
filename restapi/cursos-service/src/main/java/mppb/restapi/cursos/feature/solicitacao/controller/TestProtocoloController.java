package mppb.restapi.cursos.feature.solicitacao.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/test-protocolo")
@RequiredArgsConstructor
@Slf4j
public class TestProtocoloController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<String> test() {
        try {
            String url = "http://10.128.4.14:8082/api/protocolo";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer scantalice");
            headers.set("X-Orgao-Id", "25348");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Metadata part - Exact JSON from Postman success
            String metadataJson = "{\n" +
                    "   \"idTipoDocumentoPecaInicial\":132,\n" +
                    "   \"codClasse\":910020,\n" +
                    "   \"codAssunto\":930257,\n" +
                    "   \"resumo\":\"Solicitação gerada via sistema Cursos-Web\",\n" +
                    "   \"instancia\":\"A\",\n" +
                    "   \"interessados\":[\n" +
                    "      {\n" +
                    "         \"idTipoInteressado\":72, \n" +
                    "         \"idPessoa\":237,    \n" +
                    "         \"sigiloso\":false\n" +
                    "      }\n" +
                    "   ]\n" +
                    "}";

            HttpHeaders metadataHeaders = new HttpHeaders();
            metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> metadataPart = new HttpEntity<>(metadataJson, metadataHeaders);
            body.add("metadata", metadataPart);

            // File part - Real PDF from project
            byte[] fileContent = Files.readAllBytes(Paths.get("/opt/project/mppb-vue/cursos-web/test_curriculo.pdf"));
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.APPLICATION_PDF);
            fileHeaders.setContentDispositionFormData("file", "teste.pdf");
            HttpEntity<byte[]> filePart = new HttpEntity<>(fileContent, fileHeaders);
            body.add("file", filePart);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Iniciando teste de protocolagem RAW para: {}", url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            log.info("Resposta do MPVirtual: {} - {}", response.getStatusCode(), response.getBody());

            return response;
        } catch (Exception e) {
            log.error("Erro no teste de protocolagem: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }
}

package mppb.restapi.mobile.feature.certidaoextrajudicial.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialRequestDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.dto.CertidaoExtrajudicialResponseDto;
import mppb.restapi.mobile.feature.certidaoextrajudicial.service.CertidaoExtrajudicialService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/" + Constants.API_PREFIX + "/public/certidoes-extrajudiciais")
public class CertidaoExtrajudicialController {

    private final CertidaoExtrajudicialService certidaoExtrajudicialService;

    @PostMapping(value = "/pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> emitirPdf(@Valid @RequestBody CertidaoExtrajudicialRequestDto request) {
        ResponseEntity<byte[]> response = certidaoExtrajudicialService.emitirPdf(request);
        return pdfResponse(response);
    }

    @GetMapping(value = "/codigo/{codigo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CertidaoExtrajudicialResponseDto> consultarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(certidaoExtrajudicialService.consultarPorCodigo(codigo));
    }

    @GetMapping(value = "/codigo/{codigo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixarPdfPorCodigo(@PathVariable String codigo) {
        ResponseEntity<byte[]> response = certidaoExtrajudicialService.baixarPdfPorCodigo(codigo);
        return pdfResponse(response);
    }

    private ResponseEntity<byte[]> pdfResponse(ResponseEntity<byte[]> response) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_PDF);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition);
        }
        builder.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        return builder.body(response.getBody());
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleCertidaoServiceError(RestClientResponseException exception) {
        MediaType contentType = exception.getResponseHeaders() != null
                ? exception.getResponseHeaders().getContentType()
                : MediaType.TEXT_PLAIN;

        return ResponseEntity.status(exception.getStatusCode())
                .contentType(contentType != null ? contentType : MediaType.TEXT_PLAIN)
                .body(exception.getResponseBodyAsString());
    }
}

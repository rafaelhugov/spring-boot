package mppb.microservices.common.core.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class AbstractController {

    protected ResponseEntity<byte[]> gerarRespostaPdf(byte[] pdf, String nomeArquivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + nomeArquivo);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}

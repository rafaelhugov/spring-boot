package mppb.restapi.cursos.feature.documentos.controller;

import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.cursos.feature.documentos.dto.GeracaoDocumentoRequest;
import mppb.restapi.cursos.feature.documentos.service.DocumentosService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/cursos/{id}/documentos")
public class DocumentosController {

    private final DocumentosService documentosService;

    public DocumentosController(DocumentosService documentosService) {
        this.documentosService = documentosService;
    }

    @PostMapping("/gerar")
    public ResponseEntity<byte[]> gerarDocumento(
            @PathVariable Long id,
            @RequestBody GeracaoDocumentoRequest request) {
        
        byte[] pdfBytes = documentosService.gerarDocumento(id, request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "documento_" + id + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}

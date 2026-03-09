package mppb.restapi.cursos.feature.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;

@RestController
@RequestMapping("/api/arquivos")
@RequiredArgsConstructor
@Tag(name = "Arquivos", description = "Endpoints para gerenciamento de arquivos (GED)")
public class ArquivoController {

    private final ArquivoService arquivoService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Faz o upload de um arquivo para o GED e retorna o ID")
    public ResponseEntity<Long> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Long id = arquivoService.upload(file);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getArquivo(@PathVariable Long id) {
        return arquivoService.buscarPorId(id)
                .map(arquivo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(arquivo.getMimeType()))
                        .body(arquivo.getBytes()))
                .orElse(ResponseEntity.notFound().build());
    }
}

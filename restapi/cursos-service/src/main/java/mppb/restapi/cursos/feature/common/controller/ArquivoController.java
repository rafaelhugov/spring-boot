package mppb.restapi.cursos.feature.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.service.ArquivoSecurityUtils;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/arquivos")
@RequiredArgsConstructor
@Tag(name = "Arquivos", description = "Endpoints para gerenciamento de arquivos (GED)")
public class ArquivoController {

    private final ArquivoService arquivoService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    @Operation(summary = "Faz o upload de um arquivo para o GED e retorna o ID")
    public ResponseEntity<Long> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Long id = arquivoService.upload(file);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('cursos.meus.cursos', 'cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<byte[]> getArquivo(@PathVariable Long id) {
        Arquivo arquivo = arquivoService.buscarArquivoPrivadoAutorizado(id);
        return ArquivoSecurityUtils.secureDownloadResponse(arquivo, false);
    }
}

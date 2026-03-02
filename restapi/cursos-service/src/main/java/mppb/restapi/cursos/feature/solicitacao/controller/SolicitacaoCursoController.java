package mppb.restapi.cursos.feature.solicitacao.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoResponseDto;
import mppb.restapi.cursos.feature.solicitacao.service.SolicitacaoCursoService;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/solicitacoes")
@RequiredArgsConstructor
@Tag(name = "Solicitacoes", description = "Gerenciar as solicitações de novos cursos")
@Tag(name = "Solicitacoes", description = "Gerenciar as solicitações de novos cursos")
public class SolicitacaoCursoController {

    private final SolicitacaoCursoService service;

    @Operation(summary = "Criar Solicitação", description = "Cria uma solicitação de curso, gera o PDF (PGA) e inicia o processo no MPVirtual.")
    @PostMapping
    public ResponseEntity<SolicitacaoCursoResponseDto> criarSolicitacao(
            @Valid @RequestBody SolicitacaoCursoRequestDto dto) {
        SolicitacaoCursoResponseDto response = service.solicitarCurso(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar Solicitação", description = "Buscar uma solicitação específica")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoCursoResponseDto> buscarSolicitacao(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // Endpoint adicional para baixar o pdf mockado:
    // @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // ...
}

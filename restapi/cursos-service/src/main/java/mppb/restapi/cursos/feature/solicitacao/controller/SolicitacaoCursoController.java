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
@CrossOrigin("*")
@RequiredArgsConstructor
@Tag(name = "Solicitacoes", description = "Gerenciar as solicitações de novos cursos")
public class SolicitacaoCursoController {

    private final SolicitacaoCursoService service;

    @Operation(summary = "Criar Solicitação", description = "Cria uma solicitação de curso, gera o PDF (PGA) e inicia o processo no MPVirtual.")
    @PostMapping
    @PreAuthorize("hasAuthority('cursos.solicitar')")
    public ResponseEntity<SolicitacaoCursoResponseDto> criarSolicitacao(
            @Valid @RequestBody SolicitacaoCursoRequestDto dto) {
        SolicitacaoCursoResponseDto response = service.solicitarCurso(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar Solicitação", description = "Buscar uma solicitação específica")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes')")
    public ResponseEntity<SolicitacaoCursoResponseDto> buscarSolicitacao(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Gerar PDF", description = "Gera o PDF (PGA) da solicitação")
    @GetMapping(value = "/{id}/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes')")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        return ResponseEntity.ok(service.gerarPdf(id));
    }

    @Operation(summary = "Listar Todas", description = "Lista as solicitações conforme o papel (Solicitante vê as suas, Admin vê as enviadas)")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes')")
    public ResponseEntity<java.util.List<SolicitacaoCursoResponseDto>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @Operation(summary = "Atualizar Solicitação", description = "Atualiza os dados de uma solicitação ou rascunho")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cursos.solicitar')")
    public ResponseEntity<SolicitacaoCursoResponseDto> atualizarSolicitacao(
            @PathVariable Long id,
            @Valid @RequestBody SolicitacaoCursoRequestDto dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Operation(summary = "Excluir Rascunho", description = "Remove uma solicitação de rascunho")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('cursos.solicitar')")
    public ResponseEntity<Void> excluirSolicitacao(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Avaliar Solicitação", description = "Define o status da solicitação (Deferido/Indeferido)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('cursos.analisar.solicitacoes')")
    public ResponseEntity<SolicitacaoCursoResponseDto> avaliarSolicitacao(
            @PathVariable Long id,
            @RequestParam Long statusId) {
        return ResponseEntity.ok(service.avaliarSolicitacao(id, statusId));
    }

    @Operation(summary = "Converter em Curso", description = "Cria um curso oficial a partir de uma solicitação deferida")
    @PostMapping("/{id}/converter")
    @PreAuthorize("hasAuthority('cursos.manter.cursos')")
    public ResponseEntity<Void> converterEmCurso(@PathVariable Long id) {
        service.converterParaCurso(id);
        return ResponseEntity.ok().build();
    }
}

package mppb.restapi.cursos.feature.solicitacao.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.solicitacao.dto.ConverterSolicitacaoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.HabilitarEdicaoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoRequestDto;
import mppb.restapi.cursos.feature.solicitacao.dto.SolicitacaoCursoResponseDto;
import mppb.restapi.cursos.feature.solicitacao.service.SolicitacaoCursoService;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<SolicitacaoCursoResponseDto> buscarSolicitacao(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Gerar PDF", description = "Gera o PDF (PGA) da solicitação")
    @GetMapping(value = "/{id}/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        return ResponseEntity.ok(service.gerarPdf(id));
    }

    @Operation(summary = "Listar Todas", description = "Lista as solicitações conforme o papel (Solicitante vê as suas, Admin vê as enviadas)")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('cursos.solicitar', 'cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<Page<SolicitacaoCursoResponseDto>> listarTodas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Long minStatusId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.buscarPaginado(search, statusId, minStatusId, pageable));
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

    @Operation(summary = "Protocolar no MPVirtual", description = "Inicia manualmente o processo no MPVirtual (Ação do CEAF)")
    @PostMapping("/{id}/protocolar")
    @PreAuthorize("hasAnyAuthority('cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<SolicitacaoCursoResponseDto> protocolarSolicitacao(@PathVariable Long id) {
        return ResponseEntity.ok(service.protocolarSolicitacao(id));
    }

    @Operation(summary = "Habilitar Edição", description = "Disponibiliza uma solicitação protocolada para ajuste pelo usuário (Ação do CEAF)")
    @PatchMapping("/{id}/habilitar-edicao")
    @PreAuthorize("hasAnyAuthority('cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<SolicitacaoCursoResponseDto> habilitarEdicao(
            @PathVariable Long id,
            @Valid @RequestBody HabilitarEdicaoRequestDto dto) {
        return ResponseEntity.ok(service.habilitarEdicao(id, dto.getDescricaoAjustes()));
    }

    @Operation(summary = "Preview do Protocolo", description = "Obtém os metadados que serão enviados ao MPVirtual para fins de conferência")
    @GetMapping("/{id}/protocolo-preview")
    @PreAuthorize("hasAnyAuthority('cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<mppb.restapi.common.infrastructure.client.mpvirtual.dto.MPVirtualMetadataDto> previewProtocolo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterMetadataProtocolo(id));
    }

    @Operation(summary = "Converter em Curso", description = "Cria um curso oficial a partir de uma solicitação deferida")
    @PostMapping(value = "/{id}/converter", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('cursos.analisar.solicitacoes', 'cursos.manter.cursos')")
    public ResponseEntity<java.util.Map<String, Object>> converterEmCurso(
            @PathVariable Long id,
            @RequestBody(required = false) ConverterSolicitacaoRequestDto request) {
        java.util.List<Long> cursoIds = service.converterParaCursos(
                id,
                request != null ? request.getMunicipioIds() : null);
        return ResponseEntity.ok(java.util.Map.of(
                "id", cursoIds.get(0),
                "ids", cursoIds));
    }
}

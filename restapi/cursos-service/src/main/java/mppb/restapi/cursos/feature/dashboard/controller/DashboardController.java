package mppb.restapi.cursos.feature.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.dashboard.dto.DashboardStatsDto;
import mppb.restapi.cursos.feature.dashboard.service.DashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints para a página inicial (Dashboard) do sistema")
@PreAuthorize("hasAuthority('cursos.dashboard')")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obtém estatísticas", description = "Retorna os indicadores para a área de destaque da dashboard.")
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @Operation(summary = "Lista cursos da vitrine", description = "Lista cursos publicados e disponíveis, com ordenação específica.")
    @GetMapping(value = "/cursos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<CursoDto>> getCursos(
            @RequestParam(required = false) Long modalidadeId,
            @RequestParam(required = false) Long publicoAlvoId,
            @RequestParam(required = false) String periodo,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getCursos(modalidadeId, publicoAlvoId, periodo, pageable));
    }
}

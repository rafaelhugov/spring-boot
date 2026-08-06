package br.gov.mppb.ncap.feature.entidade;

import br.gov.mppb.ncap.feature.entidade.converter.EntidadeConverter;
import br.gov.mppb.ncap.feature.entidade.dto.EntidadeDto;
import br.gov.mppb.ncap.feature.entidade.integration.EntidadeSincronizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/entidades")
@RequiredArgsConstructor
@Validated
@Tag(name = "Entidades", description = "Endpoints de consulta e gerenciamento de entidades fiscalizadas")
public class EntidadeController extends AbstractController {

    private final EntidadeService service;
    private final EntidadeConverter converter;
    private final EntidadeSincronizacaoService syncService;

    @Operation(summary = "Listar entidades")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ceap.acesso', 'ceap.inspecoes', 'ceap.manter.entidades')")
    public ResponseEntity<Page<EntidadeDto>> listAll(@ParameterObject Pageable pageable, @RequestParam(required = false) String search) {
        log.info("Listar entidades. Filtro: {}", search);
        return ResponseEntity.ok(service.findAll(search, pageable).map(converter::toDto));
    }

    @Operation(summary = "Buscar entidade por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ceap.acesso', 'ceap.inspecoes', 'ceap.manter.entidades')")
    public ResponseEntity<EntidadeDto> findById(@PathVariable Long id) {
        log.info("Buscar entidade ID: {}", id);
        return service.findById(id)
                .map(e -> ResponseEntity.ok(converter.toDto(e)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar entidade")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ceap.manter.entidades')")
    public ResponseEntity<EntidadeDto> update(@PathVariable Long id, @RequestBody EntidadeDto dto) {
        log.info("Atualizar entidade ID: {}", id);
        Entidade atualizada = service.update(id, dto);
        return ResponseEntity.ok(converter.toDto(atualizada));
    }

    @Operation(summary = "Disparar sincronização manual com o CNMP")
    @PostMapping("/sincronizar")
    @PreAuthorize("hasAuthority('ceap.manter.entidades')")
    public ResponseEntity<Void> sincronizar() {
        log.info("Disparando sincronização manual com o CNMP...");
        syncService.sincronizar();
        return ResponseEntity.ok().build();
    }
}

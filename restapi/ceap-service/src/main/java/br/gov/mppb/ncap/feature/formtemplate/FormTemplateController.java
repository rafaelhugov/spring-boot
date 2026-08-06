package br.gov.mppb.ncap.feature.formtemplate;

import br.gov.mppb.ncap.feature.formtemplate.converter.FormTemplateConverter;
import br.gov.mppb.ncap.feature.formtemplate.dto.FormTemplateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/form-templates")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyAuthority('ceap.acesso', 'ceap.inspecoes')")
@Tag(name = "Templates de Formulário", description = "Endpoints de gerenciamento e consulta de templates de formulários do CNMP")
public class FormTemplateController extends AbstractController {

    private final FormTemplateService service;
    private final FormTemplateConverter converter;

    @Operation(summary = "Sincronizar templates de formulários do CNMP manualmente")
    @PostMapping(value = "/sincronizar")
    public ResponseEntity<Void> sincronizar() {
        log.info("Sincronização manual de templates de formulários solicitada.");
        service.sincronizar();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar templates de formulários ativos")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FormTemplateDto>> listAll() {
        log.info("Listar templates de formulários ativos.");
        List<FormTemplateDto> list = service.findAllActive().stream()
                .map(converter::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Obter template de formulário por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FormTemplateDto> findById(@PathVariable Long id) {
        log.info("Buscar template de formulário ID: {}", id);
        return service.findById(id)
                .map(e -> ResponseEntity.ok(converter.toDto(e)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package mppb.microservices.fundoapi.feature.cobranca.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.core.error.NotFoundException;
import mppb.microservices.common.core.controller.AbstractController;
import mppb.microservices.common.core.util.Constants;
import mppb.microservices.fundoapi.feature.cobranca.converter.AreaConverter;
import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import mppb.microservices.fundoapi.feature.cobranca.service.AreaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/areas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Áreas", description = "Gerencia as operações relacionadas a áreas")
public class AreaController extends AbstractController {

    private final AreaService areaService;

    private final AreaConverter areaConverter;

    @Operation(summary = "Obtém áreas", description = "Retorna uma lista de áreas.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AreaDto>> buscaAreas() {
        return ResponseEntity.ok(areaService.findAll().stream().map(areaConverter::toDto).toList());
    }

    @GetMapping(value="/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AreaDto> getById(@PathVariable Long id) {
        return areaService.findById(id)
                .map(areaConverter::toDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Área não encontrada: " + id));
    }

}

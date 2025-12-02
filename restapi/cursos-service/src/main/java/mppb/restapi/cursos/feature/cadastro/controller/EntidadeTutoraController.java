package mppb.restapi.cursos.feature.cadastro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.cursos.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.cadastro.service.EntidadeTutoraService;
import mppb.restapi.cursos.infrastructure.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/entidades-tutoras")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entidades tutoras", description = "Gerencia as operações relacionadas a entidades tutoras")
public class EntidadeTutoraController extends AbstractController {

    private final EntidadeTutoraService entidadeTutoraService;
    private final DtoConverter<EntidadeTutoraDto, EntidadeTutora> entidadeTutoraConverter;

    @Operation(summary = "Obtém entidades tutoras", description = "Retorna entidades tutoras paginadas.")
    @GetMapping(value="", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntidadeTutoraDto>> getPages(Pageable pageable, @RequestParam(required = false) String nome) {
        return ResponseEntity.ok(entidadeTutoraService.findAll(nome, pageable).map(entidadeTutoraConverter::toDto));
    }

    @PostMapping
    public ResponseEntity<EntidadeTutoraDto> create(@RequestBody EntidadeTutoraDto areaDto) {
        EntidadeTutora createdEntity = entidadeTutoraService.save(entidadeTutoraConverter.toEntity(areaDto));
        return new ResponseEntity<>(entidadeTutoraConverter.toDto(createdEntity), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntidadeTutoraDto> update(@PathVariable Long id, @RequestBody EntidadeTutoraDto entidadeTutoraDto) {
        return entidadeTutoraService.findById(id)
                .map(existing -> {
                    entidadeTutoraDto.setId(id); // Garante que o ID do path é o mesmo do objeto
                    EntidadeTutora updatedEntity = entidadeTutoraService.save(entidadeTutoraConverter.toEntity(entidadeTutoraDto));
                    return ResponseEntity.ok(entidadeTutoraConverter.toDto(updatedEntity));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return entidadeTutoraService.findById(id)
                .map(entidadeTutora -> {
                    entidadeTutoraService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
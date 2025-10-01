package mppb.restapi.cursos.feature.curso.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.validation.groups.Creation;
import mppb.restapi.common.core.validation.groups.UpdateSequence;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.curso.dto.CursoView;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.infrastructure.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/cursos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cursos", description = "Gerencia as operações relacionadas a cursos")
public class CursoController extends AbstractController {

    private final CursoService cursoService;

    private final DtoConverter<CursoDto, Curso> cursoConverter;

    @Operation(summary = "Obtém cursos", description = "Retorna cursos paginados.")
    @GetMapping(value="", produces =  MediaType.APPLICATION_JSON_VALUE)
    @JsonView(CursoView.Listagem.class)
    public ResponseEntity<Page<CursoDto>> getPages(
                                                   @RequestParam(required = false) String titulo,
                                                   @RequestParam(required = false) Long modalidadeId,
                                                   @RequestParam(required = false) Long publicoAlvoId,
                                                   Pageable pageable) {
        return ResponseEntity.ok(cursoService.findAll(titulo, modalidadeId, publicoAlvoId, pageable).map(cursoConverter::toDto));
    }

    @Operation(summary = "Obtém curso", description = "Retorna curso.")
    @GetMapping(value="/{id}", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CursoDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.findById(id).map(cursoConverter::toDto).orElseThrow(() ->new NotFoundException("Curso não encontrado: " + id)));
    }

    @PostMapping
    public ResponseEntity<CursoDto> create(@Validated({Creation.class, Default.class}) @RequestBody CursoDto cursoDto) {
        Curso createdEntity = cursoService.save(cursoConverter.toEntity(cursoDto));
        return new ResponseEntity<>(cursoConverter.toDto(createdEntity), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoDto> update(@PathVariable Long id, @Validated(UpdateSequence.class) @RequestBody CursoDto cursoDto) {
        return cursoService.findById(id)
                .map(existing -> {
                    cursoDto.setId(id); // Garante que o ID do path é o mesmo do objeto
                    Curso updatedEntity = cursoService.save(cursoConverter.toEntity(cursoDto));
                    return ResponseEntity.ok(cursoConverter.toDto(updatedEntity));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return cursoService.findById(id)
                .map(curso -> {
                    cursoService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package mppb.restapi.cursos.feature.ministrante.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.validation.groups.Update;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteDto;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteSearchResultDto;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteView;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import mppb.restapi.cursos.feature.ministrante.service.MinistranteService;
import mppb.restapi.cursos.infrastructure.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/ministrantes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ministrantes", description = "Gerencia as operações relacionadas a ministrantes")
public class MinistranteController extends AbstractController {

    private final MinistranteService ministranteService;
    private final DtoConverter<MinistranteDto, Ministrante> ministranteConverter;

    @Operation(summary = "Obtém ministrantes", description = "Retorna ministrantes paginados.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(MinistranteView.Listagem.class)
    public ResponseEntity<Page<MinistranteDto>> getPages(Pageable pageable) {
        Page<Ministrante> ministrantePage = ministranteService.findAll(pageable);
        //TODO usar uma projection para associar os dados, ao inves de usar o enrich. Para que faça apenas uma consulta
        return ResponseEntity.ok(ministrantePage.map(ministranteService::toEnrichedDto));
    }

    @Operation(summary = "Obtém ministrante", description = "Retorna um ministrante específico pelo seu ID.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(MinistranteView.Detalhes.class)
    public ResponseEntity<MinistranteDto> get(@PathVariable Long id) {
        return ministranteService.findById(id)
                .map(ministranteService::toEnrichedDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Ministrante não encontrado: " + id));
    }

    @Operation(summary = "Cria um novo ministrante", description = "Cria um novo ministrante, seja interno ou externo.")
    @PostMapping
    public ResponseEntity<MinistranteDto> create(@Validated({Default.class}) @RequestBody MinistranteDto ministranteDto) {
        Ministrante ministranteParaSalvar = ministranteConverter.toEntity(ministranteDto);
        Ministrante ministranteSalvo = ministranteService.save(ministranteParaSalvar);
        MinistranteDto dtoDeRetorno = ministranteService.toEnrichedDto(ministranteSalvo);
        return new ResponseEntity<>(dtoDeRetorno, HttpStatus.CREATED);
    }

    @Operation(summary = "Atualiza um ministrante", description = "Atualiza os dados de um ministrante existente.")
    @PutMapping("/{id}")
    public ResponseEntity<MinistranteDto> update(@PathVariable Long id, @Validated(Update.class) @RequestBody MinistranteDto ministranteDto) {
        return ministranteService.findById(id)
                .map(existing -> {
                    ministranteDto.setId(id);
                    Ministrante ministranteParaAtualizar = ministranteConverter.toEntity(ministranteDto);
                    Ministrante ministranteAtualizado = ministranteService.save(ministranteParaAtualizar);
                    MinistranteDto dtoDeRetorno = ministranteService.toEnrichedDto(ministranteAtualizado);
                    return ResponseEntity.ok(dtoDeRetorno);
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Exclui um ministrante", description = "Exclui um ministrante pelo seu ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        ministranteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    //TODO limitar quantidade de resultados 10
    @Operation(summary = "Busca unificada de ministrantes", description = "Retorna uma lista de ministrantes internos (servidores do RH) e externos (já cadastrados) que correspondem ao termo de busca.")
    @GetMapping("/search")
    public ResponseEntity<List<MinistranteSearchResultDto>> searchMinistrantes(
            @RequestParam(name = "nome", required = true) String searchTerm) {

        // Adiciona uma verificação para não buscar com termo vazio
        if (searchTerm == null || searchTerm.trim().length() < 3) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<MinistranteSearchResultDto> results = ministranteService.searchMinistrantes(searchTerm);
        return ResponseEntity.ok(results);
    }
}


package mppb.restapi.eleicao.feature.eleicao.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.eleicao.feature.eleicao.dto.EleicaoDto;
import mppb.restapi.eleicao.feature.eleicao.dto.ResultadoDto;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import mppb.restapi.eleicao.feature.eleicao.service.EleicaoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/eleicoes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Eleições", description = "Gerencia as operações relacionadas a eleições")
public class EleicaoController extends AbstractController {

    private final EleicaoService eleicaoService;
    private final DtoConverter<EleicaoDto, Eleicao> eleicaoConverter;

    @Operation(summary = "Lista as eleições", description = "Listagem das eleições (paginadas).")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EleicaoDto>> getPages(Pageable pageable) {
        return ResponseEntity.ok(eleicaoService.findAll(pageable).map(eleicaoConverter::toDto));
    }

    @Operation(summary = "Obtém eleição", description = "Retorna informações da eleição.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EleicaoDto> findById(@PathVariable Long id) {
        return eleicaoService.findById(id)
                .map(eleicao -> ResponseEntity.ok(eleicaoConverter.toDto(eleicao)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Cadastra uma eleição", description = "Cadastra uma nova eleição.")
    @PostMapping
    @PreAuthorize("hasAuthority('eleicao.manter.eleicoes')")
    public ResponseEntity<EleicaoDto> create(@Valid @RequestBody EleicaoDto eleicaoDto) {
        Eleicao createdEntity = eleicaoService.save(eleicaoConverter.toEntity(eleicaoDto));
        return new ResponseEntity<>(eleicaoConverter.toDto(createdEntity), HttpStatus.CREATED);
    }

    @Operation(summary = "Atualiza uma eleição", description = "Atualiza as informações de uma eleição.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('eleicao.manter.eleicoes')")
    public ResponseEntity<EleicaoDto> update(@PathVariable Long id, @Valid @RequestBody EleicaoDto eleicaoDto) {
        return eleicaoService.findById(id)
                .map(existing -> {
                    eleicaoDto.setId(id);
                    Eleicao updatedEntity = eleicaoConverter.toEntity(eleicaoDto);
                    Eleicao saved = eleicaoService.save(updatedEntity);
                    return ResponseEntity.ok(eleicaoConverter.toDto(saved));
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Deleta uma eleição", description = "Deleta uma eleição (exclusão lógica).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('eleicao.manter.eleicoes')")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return eleicaoService.findById(id)
                .map(eleicao -> {
                    eleicaoService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Encerra uma eleição", description = "Força o encerramento da votação antecipadamente.")
    @PostMapping("/{id}/encerrar")
    @PreAuthorize("hasAuthority('eleicao.encerrar.votacao')")
    public ResponseEntity<Void> encerrar(@PathVariable Long id) {
        eleicaoService.encerrarVotacao(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resultado da eleição", description = "Retorna a apuração dos votos da eleição.")
    @GetMapping(value = "/{id}/resultado", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoDto> getResultado(@PathVariable Long id) {
        return ResponseEntity.ok(eleicaoService.calcularResultado(id));
    }
}

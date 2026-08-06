package br.gov.mppb.ncap.feature.inspecao;

import br.gov.mppb.ncap.feature.inspecao.converter.InspecaoConverter;
import br.gov.mppb.ncap.feature.inspecao.converter.RespostaInspecaoConverter;
import br.gov.mppb.ncap.feature.inspecao.dto.InspecaoDto;
import br.gov.mppb.ncap.feature.inspecao.dto.RespostaInspecaoDto;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.validation.groups.Create;
import mppb.restapi.common.core.validation.groups.Update;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/inspecoes")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('ceap.inspecoes')")
@Tag(name = "Inspeções", description = "Endpoints de gerenciamento do ciclo de vida das inspeções do NCAP")
public class InspecaoController extends AbstractController {

    private final InspecaoService service;
    private final InspecaoConverter converter;
    private final RespostaInspecaoConverter respostaConverter;


    @Operation(summary = "Listar inspeções")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<InspecaoDto>> listAll(@ParameterObject Pageable pageable, @RequestParam(required = false) String search) {
        log.info("Listar inspeções. Filtro: {}", search);
        return ResponseEntity.ok(service.findAll(search, pageable).map(converter::toDto));
    }

    @Operation(summary = "Buscar inspeção por ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> findById(@PathVariable Long id) {
        log.info("Buscar inspeção ID: {}", id);
        return service.findById(id)
                .map(e -> ResponseEntity.ok(converter.toDto(e)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Criar nova inspeção")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> save(@Validated({Create.class, Default.class}) @RequestBody InspecaoDto dto) {
        log.info("Criar inspeção. Entidade: {}", dto.getEntidadeId());
        Inspecao salva = service.save(converter.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(converter.toDto(salva));
    }

    @Operation(summary = "Atualizar inspeção")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> update(@PathVariable Long id,
            @Validated({Update.class, Default.class}) @RequestBody InspecaoDto dto) {
        log.info("Atualizar inspeção ID: {}", id);
        return service.findById(id)
                .map(existente -> {
                    dto.setId(id);
                    Inspecao atualizada = service.save(converter.toEntity(dto));
                    return ResponseEntity.ok(converter.toDto(atualizada));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Remarcar inspeção", description = "Altera a data e valida enquadramento semestral (Res. 279/2023 CNMP)")
    @PostMapping(value = "/{id}/remarcar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> remarcar(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate novaData) {
        log.info("Remarcar inspeção ID: {} para {}", id, novaData);
        try {
            return ResponseEntity.ok(converter.toDto(service.remarcar(id, novaData)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Renovar token de acesso externo", description = "Revoga o token atual e gera um novo link seguro para a delegacia")
    @PostMapping(value = "/{id}/renovar-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> renovarToken(@PathVariable Long id) {
        log.info("Renovar token externo da inspeção ID: {}", id);
        try {
            return ResponseEntity.ok(converter.toDto(service.gerarNovoToken(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Deletar inspeção")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deletar inspeção ID: {}", id);
        return service.findById(id)
                .map(e -> {
                    service.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obter respostas da inspeção")
    @GetMapping(value = "/{id}/respostas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RespostaInspecaoDto>> obterRespostas(@PathVariable Long id) {
        log.info("Obter respostas da inspeção ID: {}", id);
        try {
            List<RespostaInspecaoDto> respostas = service.obterRespostasInspecao(id).stream()
                    .map(respostaConverter::toDto)
                    .toList();
            return ResponseEntity.ok(respostas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Salvar ou atualizar respostas da inspeção")
    @PutMapping(value = "/{id}/respostas", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RespostaInspecaoDto>> salvarRespostas(@PathVariable Long id,
            @RequestBody List<RespostaInspecaoDto> dtos) {
        log.info("Salvar respostas da inspeção ID: {}. Quantidade: {}", id, dtos.size());
        try {
            List<RespostaInspecao> entities = dtos.stream()
                    .map(respostaConverter::toEntity)
                    .toList();
            List<RespostaInspecaoDto> salvas = service.salvarRespostas(id, entities).stream()
                    .map(respostaConverter::toDto)
                    .toList();
            return ResponseEntity.ok(salvas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

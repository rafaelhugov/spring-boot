package mppb.microservices.fundoapi.feature.cobranca.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.core.controller.AbstractController;
import mppb.microservices.common.core.util.Constants;
import mppb.microservices.fundoapi.feature.cobranca.converter.NaturezaConverter;
import mppb.microservices.fundoapi.feature.cobranca.dto.NaturezaDto;
import mppb.microservices.fundoapi.feature.cobranca.service.NaturezaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/naturezas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Naturezas", description = "Gerencia as operações relacionadas a naturezas")
public class NaturezaController extends AbstractController {

    private final NaturezaService naturezaService;

    private final NaturezaConverter naturezaConverter;


    @Operation(
            summary = "Obtém todas as naturezas",
            description = "Retorna uma lista de todas as naturezas cadastradas.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de naturezas retornada com sucesso",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = NaturezaDto.class))
                    )
            }
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NaturezaDto>> buscaTodos() {
        return ResponseEntity.ok(naturezaService.findAll().stream().map(naturezaConverter::toDto).toList());
    }

    @Operation(
            summary = "Obtém natureza por ID",
            description = "Retorna a natureza correspondente ao ID informado.",
            parameters = {
                    @Parameter(
                            name = "id",
                            description = "ID da natureza",
                            required = true,
                            example = "1"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Natureza encontrada com sucesso",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = NaturezaDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Natureza não encontrada",
                            content = @Content
                    )
            }
    )
    @GetMapping(value="/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NaturezaDto> getById(@PathVariable Long id) {
        return naturezaService.findById(id)
                .map(naturezaConverter::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}

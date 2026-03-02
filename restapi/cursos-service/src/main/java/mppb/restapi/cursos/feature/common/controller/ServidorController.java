package mppb.restapi.cursos.feature.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/servidores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Servidores", description = "Endpoints para consulta de servidores.")
public class ServidorController extends AbstractController {

    private final ServidorService servidorService;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca servidores por nome", description = "Retorna uma lista de até 10 servidores cujo nome contém o termo pesquisado.")
    public ResponseEntity<List<ServidorProjection>> buscarServidoresPorNome(
            @Parameter(description = "Termo de busca para o nome do servidor.", required = true, example = "joao silva")
            @RequestParam("nome") @NotBlank String nome) {

        List<ServidorProjection> servidores = servidorService.buscarServidoresPorNome(nome);
        return ResponseEntity.ok(servidores);
    }


    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca um servidor por ID", description = "Retorna os dados de um servidor específico a partir do seu ID.")
    public ResponseEntity<ServidorProjection> buscarServidorPorId(
            @Parameter(description = "ID do servidor a ser buscado.", required = true, example = "12345")
            @PathVariable("id") Integer id) {
        return servidorService.buscarServidorPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
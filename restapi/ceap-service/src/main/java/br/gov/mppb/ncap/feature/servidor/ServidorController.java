package br.gov.mppb.ncap.feature.servidor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/servidores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ceap.acesso', 'ceap.inspecoes')")
@Tag(name = "Servidores", description = "Consulta de servidores/membros do MPPB para composição da equipe de inspeção")
public class ServidorController extends AbstractController {

    private final ServidorService servidorService;

    @Operation(
        summary = "Buscar servidores por nome",
        description = "Retorna até 10 servidores/membros ativos que correspondam ao termo informado (mínimo 3 caracteres)."
    )
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ServidorDto>> search(@RequestParam String nome) {
        log.info("Busca de servidores por nome: '{}'", nome);

        if (nome == null || nome.trim().length() < 3) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<ServidorDto> result = servidorService.buscarPorNome(nome.trim()).stream()
                .map(p -> ServidorDto.builder()
                        .id(p.getId())
                        .nome(p.getNome())
                        .cargoNome(p.getCargoNome())
                        .tipoCargoNome(p.getTipoCargoNome())
                        .servidorMatricula(p.getServidorMatricula())
                        .situacao(p.getSituacao())
                        .email(p.getEmail())
                        .build())
                .toList();

        return ResponseEntity.ok(result);
    }
}

package mppb.restapi.eleicao.feature.voto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.eleicao.feature.voto.dto.VotoDto;
import mppb.restapi.eleicao.feature.voto.entity.Voto;
import mppb.restapi.eleicao.feature.voto.service.VotoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votos")
@RequiredArgsConstructor
@Tag(name = "Votos", description = "Gerencia as operações de votação")
public class VotoController extends AbstractController {

    private final VotoService votoService;
    private final DtoConverter<VotoDto, Voto> votoConverter;

    @Operation(summary = "Registra um voto", description = "Realiza o registro de um voto em uma eleição.")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VotoDto> votar(@RequestBody VotoDto votoDto, HttpServletRequest request) {
        Voto voto = votoService.registrarVoto(
                votoDto.getIdEleicao(),
                votoDto.getCandidatosIds(),
                votoDto.getTipo(),
                request.getRemoteAddr()
        );
        return new ResponseEntity<>(votoConverter.toDto(voto), HttpStatus.CREATED);
    }
}

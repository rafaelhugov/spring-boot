package mppb.restapi.cursos.feature.participante.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteSearchResultDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteView;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/participantes")
@RequiredArgsConstructor
@Tag(name = "Participantes", description = "Gerencia as operações relacionadas a participantes")
@PreAuthorize("hasAuthority('cursos.manter.cursos')")
public class ParticipanteController {

    private final ParticipanteService participanteService;

    @Operation(
            summary = "Busca unificada de participantes",
            description = "Retorna uma lista de participantes internos (servidores do RH) e externos (já cadastrados) que correspondem ao termo de busca."
    )
    @GetMapping("/search")
    @JsonView(ParticipanteView.Listagem.class)
    public ResponseEntity<List<ParticipanteSearchResultDto>> search(
            @RequestParam(name = "nome") String searchTerm) {

        // Adiciona a mesma validação do MinistranteController
        if (searchTerm == null || searchTerm.trim().length() < 3) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<ParticipanteSearchResultDto> results = participanteService.searchParticipantes(searchTerm);
        return ResponseEntity.ok(results);
    }
}


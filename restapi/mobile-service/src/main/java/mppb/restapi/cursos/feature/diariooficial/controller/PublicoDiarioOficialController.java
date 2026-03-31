package mppb.restapi.cursos.feature.diariooficial.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.cursos.feature.diariooficial.converter.DiarioOficialConverter;
import mppb.restapi.cursos.feature.diariooficial.dto.DiarioOficialDto;
import mppb.restapi.cursos.feature.diariooficial.entity.DiarioOficial;
import mppb.restapi.cursos.feature.diariooficial.service.DiarioOficialService;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/public/diarios-oficiais")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Diário Oficial - Público", description = "Endpoints públicos sobre o Diário Oficial Eletrônico.")
public class PublicoDiarioOficialController extends AbstractController {

    private final DiarioOficialService diarioOficialService;
    private final DiarioOficialConverter diarioOficialConverter;


    @GetMapping(value = "/recentes", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DiarioOficialDto>> listaTeste() {
        List<DiarioOficial> diariosOficiais = diarioOficialService.recuperarDiariosOficiaisRecentes();
        List<DiarioOficialDto> diariosOficiaisDto = diariosOficiais.stream().map(e->diarioOficialConverter.toDto(e)).collect(Collectors.toList());
        return ResponseEntity.ok(diariosOficiaisDto);
    }

}

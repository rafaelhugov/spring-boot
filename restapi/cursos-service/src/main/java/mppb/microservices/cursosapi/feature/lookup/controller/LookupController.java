package mppb.microservices.cursosapi.feature.lookup.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.core.controller.AbstractController;
import mppb.microservices.common.core.converter.EntityConverter;
import mppb.microservices.cursosapi.feature.lookup.converter.ModalidadeConverter;
import mppb.microservices.cursosapi.feature.lookup.dto.ModalidadeDto;
import mppb.microservices.cursosapi.feature.lookup.dto.PapelMinistranteDto;
import mppb.microservices.cursosapi.feature.lookup.dto.PublicoAlvoDto;
import mppb.microservices.cursosapi.feature.lookup.dto.TipoEventoDto;
import mppb.microservices.cursosapi.feature.lookup.entities.PapelMinistrante;
import mppb.microservices.cursosapi.feature.lookup.entities.PublicoAlvo;
import mppb.microservices.cursosapi.feature.lookup.entities.TipoEvento;
import mppb.microservices.cursosapi.feature.lookup.service.LookupService;
import mppb.microservices.cursosapi.infrastructure.util.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/lookup")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Controlador lookup", description = "Retorna lista de seleção.")
public class LookupController extends AbstractController {

    private final LookupService lookupService;
    private final ModalidadeConverter modalidadeConverter;
    private final EntityConverter<TipoEventoDto, TipoEvento> tipoEventoConverter;
    private final EntityConverter<PublicoAlvoDto, PublicoAlvo> publicoAlvoConverter;
    private final EntityConverter<PapelMinistranteDto, PapelMinistrante> papelMinistranteConverter;

    @GetMapping(value = "/modalidades", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ModalidadeDto>> listModalidades() {
        return ResponseEntity.ok(lookupService.findAllModalidades().stream().map(modalidadeConverter::toDto).toList());
    }

    @GetMapping(value = "/tipos-evento", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TipoEventoDto>> listTiposEvento() {
        return ResponseEntity.ok(lookupService.findAllTiposEvento().stream().map(tipoEventoConverter::toDto).toList());
    }

    @GetMapping(value = "/publico-alvo", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PublicoAlvoDto>> listPublicoAlvo() {
        return ResponseEntity.ok(lookupService.findAllPublicoAlvo().stream().map(publicoAlvoConverter::toDto).toList());
    }

    @GetMapping(value = "/papeis-ministrante", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PapelMinistranteDto>> listPapeisPalestrante() {
        return ResponseEntity.ok(lookupService.findAllPapelMinistrante().stream().map(papelMinistranteConverter::toDto).toList());
    }

}

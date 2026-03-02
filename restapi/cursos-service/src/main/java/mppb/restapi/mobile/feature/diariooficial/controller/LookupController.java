package mppb.restapi.mobile.feature.diariooficial.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.mobile.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.restapi.mobile.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.mobile.feature.lookup.converter.ModalidadeConverter;
import mppb.restapi.mobile.feature.lookup.dto.*;
import mppb.restapi.mobile.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.mobile.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.mobile.feature.lookup.entities.TipoEvento;
import mppb.restapi.mobile.feature.lookup.service.LookupService;
import mppb.restapi.mobile.infrastructure.util.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Lookup", description = "Retorna lista de seleção.")
@PreAuthorize("hasAuthority('cursos.cadastros')")
//TODO cachear esse controller
public class LookupController extends AbstractController {

    private final LookupService lookupService;
    private final ModalidadeConverter modalidadeConverter;
    private final DtoConverter<TipoEventoDto, TipoEvento> tipoEventoConverter;
    private final DtoConverter<PublicoAlvoDto, PublicoAlvo> publicoAlvoConverter;
    private final DtoConverter<PapelMinistranteDto, PapelMinistrante> papelMinistranteConverter;
    private final DtoConverter<EntidadeTutoraDto, EntidadeTutora> entidadeTutoraConverter;


    @GetMapping(value = "/modalidades", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ModalidadeDto>> listModalidades() {
        return ResponseEntity.ok(lookupService.findAllModalidades().stream().map(modalidadeConverter::toDto).toList());
    }

    @GetMapping(value = "/tipos-evento", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TipoEventoDto>> listTiposEvento() {
        return ResponseEntity.ok(lookupService.findAllTiposEvento().stream().map(tipoEventoConverter::toDto).toList());
    }

    @GetMapping(value = "/entidades-tutoras", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EntidadeTutoraDto>> listEntidadesTutoras() {
        return ResponseEntity.ok(lookupService.findAllEntidadesTutoras().stream().map(entidadeTutoraConverter::toDto).toList());
    }

    @GetMapping(value = "/publico-alvo", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PublicoAlvoDto>> listPublicoAlvo() {
        return ResponseEntity.ok(lookupService.findAllPublicoAlvo().stream().map(publicoAlvoConverter::toDto).toList());
    }

    @GetMapping(value = "/papeis-ministrantes", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PapelMinistranteDto>> listPapeisPalestrante() {
        return ResponseEntity.ok(lookupService.findAllPapelMinistrante().stream().map(papelMinistranteConverter::toDto).toList());
    }

    @GetMapping(value = "/situacoes-inscricao", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SituacaoInscricaoDto>> listSituacoesInscricao() {
        return ResponseEntity.ok(
                lookupService.findAllSituacoesInscricao().stream()
                        .map(inscricao -> new SituacaoInscricaoDto(inscricao.getCodigo(), inscricao.getDescricao()))
                        .toList()
        );
    }

}

package mppb.microservices.cursosapi.feature.lookup.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.core.controller.AbstractController;
import mppb.microservices.cursosapi.feature.lookup.converter.ModalidadeConverter;
import mppb.microservices.cursosapi.feature.lookup.dto.ModalidadeDto;
import mppb.microservices.cursosapi.feature.lookup.service.LookupService;
import mppb.microservices.cursosapi.infrastructure.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.cache.annotation.Cacheable;

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

    @GetMapping("/modalidades")
    public ResponseEntity<List<ModalidadeDto>> getModalidades() {
        return ResponseEntity.ok(lookupService.findAllModalidades().stream().map(modalidadeConverter::toDto).toList());
    }


}

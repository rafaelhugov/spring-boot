package mppb.restapi.mobile.feature.lookup.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.mobile.infrastructure.util.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/lookup")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lookup", description = "Retorna lista de seleção.")
//@PreAuthorize("hasAuthority('cursos.cadastros')")
public class LookupController extends AbstractController {

    @GetMapping(value = "/teste", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listaTeste() {
        Map<String, Object> map = new HashMap<>();
        map.put("chave1", "valor1");
        map.put("chave2", "valor2");
        return ResponseEntity.ok(map);
    }

}

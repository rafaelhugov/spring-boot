package mppb.restapi.mobile.feature.perfil.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(Constants.API_PREFIX + "/perfil")
@RequiredArgsConstructor
public class PerfilUsuarioController {

    private final PerfilUsuarioService service;

    @GetMapping("/dados")
    public ResponseEntity<PerfilUsuarioDto> dados() {
        try {
            String token = AutenticacaoAtual.token().orElse(null);
            if (token == null) {
                return ResponseEntity.status(401).build();
            }
            PerfilUsuarioDto perfil = service.buscarPerfilAutenticado(token);
            if (perfil == null) {
                return ResponseEntity.status(401).build();
            }
            return ResponseEntity.ok(perfil);
        } catch (Exception e) {
            log.error("Erro ao buscar perfil do usuario autenticado", e);
            return ResponseEntity.status(500).build();
        }
    }
}

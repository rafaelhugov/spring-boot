package mppb.restapi.mobile.feature.synthetic.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;

@Slf4j
@RestController
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@CrossOrigin(origins = "*")
@RequestMapping({"/acesso", "/" + Constants.API_PREFIX + "/acesso"})
public class AutenticacaoSinteticaController {

    private final AutenticadorUsuarioSintetico autenticador;

    @Autowired
    public AutenticacaoSinteticaController(AutenticadorUsuarioSintetico autenticador) {
        this.autenticador = autenticador;
    }

    @PostMapping(
            value = "/autoriza",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> autoriza(@Valid @RequestBody AutorizaRequestDto request) {
        if (request == null || request.getLogin() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"security.invalid-credentials\"}");
        }

        // Se o usuário for uma das 6 identidades sintéticas permitidas
        if (autenticador.isLoginSintetico(request.getLogin())) {
            Optional<String> token = autenticador.autenticar(request.getLogin(), request.getSenha());
            if (token.isPresent()) {
                AutorizaResponseDto response = new AutorizaResponseDto();
                response.setToken(token.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"security.invalid-credentials\"}");
        }

        // Zero proxy institucional: qualquer usuário não sintético é rejeitado com 401 controlado
        log.warn("Tentativa de acesso com usuario nao sintetico em endpoint de teste UAT: {}", request.getLogin());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("{\"error\":\"security.invalid-credentials\"}");
    }
}

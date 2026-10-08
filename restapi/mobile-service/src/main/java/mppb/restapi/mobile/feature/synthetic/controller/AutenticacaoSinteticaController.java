package mppb.restapi.mobile.feature.synthetic.controller;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
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
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorStoreReview;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.config.StoreReviewProperties;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;

@Slf4j
@RestController
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@CrossOrigin(origins = "*")
@RequestMapping({"/acesso", "/" + Constants.API_PREFIX + "/acesso"})
public class AutenticacaoSinteticaController {

    private final AutenticadorUsuarioSintetico autenticador;
    private final AutenticadorStoreReview autenticadorStoreReview;
    private final StoreReviewProperties reviewProperties;
    private final Environment environment;

    public AutenticacaoSinteticaController(AutenticadorUsuarioSintetico autenticador) {
        this(autenticador, null, null, null);
    }

    @Autowired
    public AutenticacaoSinteticaController(
            AutenticadorUsuarioSintetico autenticador,
            @Autowired(required = false) AutenticadorStoreReview autenticadorStoreReview,
            @Autowired(required = false) StoreReviewProperties reviewProperties,
            @Autowired(required = false) Environment environment) {
        this.autenticador = autenticador;
        this.autenticadorStoreReview = autenticadorStoreReview;
        this.reviewProperties = reviewProperties;
        this.environment = environment;
    }

    @PostMapping(
            value = "/autoriza",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> autoriza(@Valid @RequestBody AutorizaRequestDto request) {
        if (request == null || request.getLogin() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("error", "security.invalid-credentials"));
        }

        String login = request.getLogin().trim();
        boolean isReviewAtivo = isReviewProfile() || (reviewProperties != null && reviewProperties.isEnabled());

        // No ambiente exclusivo de Store Review (SPRING_PROFILES_ACTIVE=review ou STORE_REVIEW_ENABLED=true):
        if (isReviewAtivo) {
            // Apenas as 4 identidades exatas de Store Review são admitidas
            if (IdentidadeSintetica.isLoginReviewExato(login) && autenticadorStoreReview != null) {
                Optional<String> token = autenticadorStoreReview.autenticarReview(login, request.getSenha());
                if (token.isPresent()) {
                    AutorizaResponseDto response = new AutorizaResponseDto();
                    response.setToken(token.get());
                    return ResponseEntity.ok(response);
                }
            }
            // uat.membro, uat.servidor, institucional, desconhecido, senha incorreta -> 401 controlado
            log.warn("Tentativa de acesso rejeitada no ambiente isolado de Store Review: login={}", login);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("error", "security.invalid-credentials"));
        }

        // Ambiente UAT interno / testes sinteticos padrao:
        if (autenticador.isLoginSintetico(login)) {
            Optional<String> token = autenticador.autenticar(login, request.getSenha());
            if (token.isPresent()) {
                AutorizaResponseDto response = new AutorizaResponseDto();
                response.setToken(token.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("error", "security.invalid-credentials"));
        }

        // Zero proxy institucional: qualquer usuário não sintético é rejeitado com 401 controlado
        log.warn("Tentativa de acesso com usuario nao sintetico em endpoint de teste UAT/Review: {}", login);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(java.util.Collections.singletonMap("error", "security.invalid-credentials"));
    }

    private boolean isReviewProfile() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .filter(p -> p != null)
                .map(p -> p.trim().toLowerCase(Locale.ROOT))
                .anyMatch("review"::equals);
    }
}

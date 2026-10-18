package mppb.restapi.mobile.feature.biometria.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.feature.biometria.dto.AtivarBiometriaRequest;
import mppb.restapi.mobile.feature.biometria.dto.AtivarBiometriaResponse;
import mppb.restapi.mobile.feature.biometria.dto.AutenticarBiometriaRequest;
import mppb.restapi.mobile.feature.biometria.dto.AutenticarBiometriaResponse;
import mppb.restapi.mobile.feature.biometria.service.BiometricCredentialService;
import mppb.restapi.mobile.feature.biometria.service.BiometricCredentialService.CredencialBiometricaInvalidaException;

@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "biometria", name = "enabled", havingValue = "true")
public class BiometricCredentialController {

    private final BiometricCredentialService service;

    @PostMapping(
            path = "/api/biometria/credenciais",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AtivarBiometriaResponse> ativar(
            @Valid @RequestBody AtivarBiometriaRequest request) {
        TokenAuthentication authentication = autenticacaoAtual();
        if (authentication == null || authentication.getUsuario() == null) {
            log.warn("Ativacao biometrica recusada: autenticacao ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AtivarBiometriaResponse response = service.ativar(
                    authentication.getUsuario().getLogin(),
                    authentication.getToken(),
                    request);
            log.info("Credencial biometrica emitida: usuarioId={}, plataforma={}",
                    authentication.getUsuario().getId(), request.plataforma());
            return ResponseEntity.ok(response);
        } catch (CredencialBiometricaInvalidaException e) {
            log.warn("Ativacao biometrica recusada: usuarioId={}, motivo=credencial-invalida",
                    authentication.getUsuario().getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping(
            path = "/api/public/biometria/autenticar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AutenticarBiometriaResponse> autenticar(
            @Valid @RequestBody AutenticarBiometriaRequest request) {
        try {
            return ResponseEntity.ok(service.autenticar(request.credencial()));
        } catch (CredencialBiometricaInvalidaException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @DeleteMapping("/api/biometria/credenciais/{dispositivoId}")
    public ResponseEntity<Void> desativar(@PathVariable String dispositivoId) {
        TokenAuthentication authentication = autenticacaoAtual();
        if (authentication == null || authentication.getUsuario() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.desativar(authentication.getUsuario().getLogin(), dispositivoId);
        return ResponseEntity.noContent().build();
    }

    private static TokenAuthentication autenticacaoAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof TokenAuthentication tokenAuthentication
                ? tokenAuthentication
                : null;
    }
}

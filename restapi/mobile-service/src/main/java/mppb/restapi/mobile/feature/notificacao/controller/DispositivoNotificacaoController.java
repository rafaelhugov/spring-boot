package mppb.restapi.mobile.feature.notificacao.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.service.DispositivoFcmService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(Constants.API_PREFIX + "/notificacoes/dispositivos")
@RequiredArgsConstructor
public class DispositivoNotificacaoController {

    private final DesignacaoAuthService authService;
    private final DispositivoFcmService dispositivoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DispositivoNotificacaoResponse> registrar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RegistrarDispositivoRequest request) {
        String token = extrairBearerToken(authorization);
        if (token == null) {
            log.warn("Registro de dispositivo FCM negado: Authorization Bearer ausente ou invalido");
            return ResponseEntity.status(401).build();
        }

        String matricula = authService.matriculaAutenticada(token);
        if (matricula == null) {
            log.warn("Registro de dispositivo FCM negado: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(401).build();
        }

        try {
            return ResponseEntity.ok(dispositivoService.registrar(matricula, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao registrar dispositivo FCM", e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{identificador}")
    public ResponseEntity<Void> desativar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("identificador") String identificador) {
        String token = extrairBearerToken(authorization);
        if (token == null) {
            log.warn("Desativacao de dispositivo FCM negada: Authorization Bearer ausente ou invalido");
            return ResponseEntity.status(401).build();
        }

        String matricula = authService.matriculaAutenticada(token);
        if (matricula == null) {
            log.warn("Desativacao de dispositivo FCM negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(401).build();
        }

        try {
            dispositivoService.desativar(matricula, identificador);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao desativar dispositivo FCM", e);
            return ResponseEntity.status(500).build();
        }
    }

    private String extrairBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        String prefixo = "Bearer ";
        if (!authorization.startsWith(prefixo)) {
            return null;
        }

        String token = authorization.substring(prefixo.length()).trim();
        return token.isEmpty() ? null : token;
    }
}

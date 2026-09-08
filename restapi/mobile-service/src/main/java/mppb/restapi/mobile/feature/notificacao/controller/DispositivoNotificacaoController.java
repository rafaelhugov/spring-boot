package mppb.restapi.mobile.feature.notificacao.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.notificacao.dto.DiagnosticoDispositivoRequest;
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
        log.info("Tentativa de registro FCM recebida: identificador={}, plataforma={}, tokenLength={}, tokenHashPrefix={}",
                safe(request.identificador()), safe(request.plataforma()), length(request.tokenFcm()),
                tokenHashPrefix(request.tokenFcm()));

        String token = extrairBearerToken(authorization);
        if (token == null) {
            log.warn("Registro de dispositivo FCM negado: Authorization Bearer ausente ou invalido");
            return ResponseEntity.status(401).build();
        }

        String matricula = matriculaAutenticada(token);
        if (matricula == null) {
            log.warn("Registro de dispositivo FCM negado: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(401).build();
        }
        log.info("Tentativa de registro FCM autenticada: matricula={}, plataforma={}", matricula,
                safe(request.plataforma()));

        try {
            return ResponseEntity.ok(dispositivoService.registrar(matricula, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao registrar dispositivo FCM", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping(path = "/diagnostico", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> diagnostico(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody DiagnosticoDispositivoRequest request) {
        String token = extrairBearerToken(authorization);
        String matricula = token == null ? null : matriculaAutenticada(token);

        log.info(
                "Diagnostico registro FCM: autenticado={}, matricula={}, etapa={}, permissao={}, possuiToken={}, tamanhoToken={}, statusBackend={}, erro={}, mensagem={}",
                matricula != null, safe(matricula), safe(request.etapa()), safe(request.permissao()),
                request.possuiToken(), request.tamanhoToken(), request.statusBackend(), safe(request.erro()),
                safe(request.mensagem()));

        return ResponseEntity.accepted().build();
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

        String matricula = matriculaAutenticada(token);
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

    private String matriculaAutenticada(String token) {
        String login = loginAutenticadoNoContexto();
        if (login != null) {
            String matricula = authService.matriculaPorLogin(login);
            if (matricula != null && !matricula.isBlank()) {
                return matricula;
            }
            log.warn("Registro de dispositivo FCM sem matricula local para login autenticado: login={}", login);
            return null;
        }

        return authService.matriculaAutenticada(token);
    }

    private String loginAutenticadoNoContexto() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (authentication instanceof TokenAuthentication tokenAuthentication
                && tokenAuthentication.getUsuario() != null
                && tokenAuthentication.getUsuario().getLogin() != null
                && !tokenAuthentication.getUsuario().getLogin().isBlank()) {
            return tokenAuthentication.getUsuario().getLogin().trim();
        }
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof String login && !login.isBlank() && !"anonymousUser".equals(login)) {
            return login.trim();
        }
        return null;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }

    private static String tokenHashPrefix(String tokenFcm) {
        if (tokenFcm == null || tokenFcm.isBlank()) {
            return "-";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(tokenFcm.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(12);
            for (int i = 0; i < 6 && i < bytes.length; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "sha256-indisponivel";
        }
    }
}

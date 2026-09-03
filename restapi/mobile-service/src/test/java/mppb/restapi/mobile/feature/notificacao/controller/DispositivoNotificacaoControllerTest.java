package mppb.restapi.mobile.feature.notificacao.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.service.DispositivoFcmService;

class DispositivoNotificacaoControllerTest {

    private FakeAuthService authService;
    private FakeDispositivoFcmService dispositivoService;
    private DispositivoNotificacaoController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = new FakeAuthService();
        dispositivoService = new FakeDispositivoFcmService();
        controller = new DispositivoNotificacaoController(authService, dispositivoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void requestSemAutenticacaoRetornaUnauthorized() {
        authService.matricula = null;

        ResponseEntity<DispositivoNotificacaoResponse> response = controller.registrar(
                "Bearer token-auth",
                new RegistrarDispositivoRequest(null, "fcm-token", "ANDROID"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(dispositivoService.registrarChamado);
    }

    @Test
    void registrarUsaMatriculaObtidaDaAutenticacao() {
        authService.matricula = "12345";

        ResponseEntity<DispositivoNotificacaoResponse> response = controller.registrar(
                "Bearer token-auth",
                new RegistrarDispositivoRequest(null, "fcm-token", "ANDROID"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("12345", dispositivoService.matriculaRecebida);
        assertEquals("token-auth", authService.tokenRecebido);
    }

    @Test
    void desativarSemAutenticacaoRetornaUnauthorized() {
        authService.matricula = null;

        ResponseEntity<Void> response = controller.desativar("Bearer token-auth", "device-1");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(dispositivoService.desativarChamado);
    }

    @Test
    void requestSemAuthorizationRetornaUnauthorized() {
        ResponseEntity<DispositivoNotificacaoResponse> response = controller.registrar(
                null,
                new RegistrarDispositivoRequest(null, "fcm-token", "ANDROID"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(dispositivoService.registrarChamado);
    }

    @Test
    void bearerInvalidoRetornaUnauthorized() {
        ResponseEntity<DispositivoNotificacaoResponse> response = controller.registrar(
                "Token token-auth",
                new RegistrarDispositivoRequest(null, "fcm-token", "ANDROID"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(dispositivoService.registrarChamado);
    }

    @Test
    void tokenEmQueryParamNaoAutenticaSemHeaderBearer() {
        ResponseEntity<DispositivoNotificacaoResponse> response = controller.registrar(
                null,
                new RegistrarDispositivoRequest(null, "fcm-token", "ANDROID"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(dispositivoService.registrarChamado);
    }

    @Test
    void postComTokenEmQueryParamNaoAutentica() throws Exception {
        mockMvc.perform(post("/api/notificacoes/dispositivos")
                .queryParam("token", "token-auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "identificador": "device-1",
                          "tokenFcm": "fcm-token",
                          "plataforma": "ANDROID"
                        }
                        """))
                .andExpect(status().isUnauthorized());

        assertFalse(dispositivoService.registrarChamado);
    }

    @Test
    void deleteComTokenEmQueryParamNaoAutentica() throws Exception {
        mockMvc.perform(delete("/api/notificacoes/dispositivos/device-1")
                .queryParam("token", "token-auth"))
                .andExpect(status().isUnauthorized());

        assertFalse(dispositivoService.desativarChamado);
    }

    @Test
    void desativarUsaMatriculaAutenticadaMantendoProtecaoIdor() {
        authService.matricula = "12345";

        ResponseEntity<Void> response = controller.desativar("Bearer token-auth", "device-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("12345", dispositivoService.matriculaRecebida);
        assertEquals("device-1", dispositivoService.identificadorRecebido);
    }

    private static class FakeAuthService extends DesignacaoAuthService {

        private String matricula = "12345";
        private String tokenRecebido;

        FakeAuthService() {
            super(null, null);
        }

        @Override
        public String matriculaAutenticada(String token) {
            tokenRecebido = token;
            return matricula;
        }
    }

    private static class FakeDispositivoFcmService extends DispositivoFcmService {

        private boolean registrarChamado;
        private boolean desativarChamado;
        private String matriculaRecebida;
        private String identificadorRecebido;

        FakeDispositivoFcmService() {
            super(null);
        }

        @Override
        public DispositivoNotificacaoResponse registrar(String matricula, RegistrarDispositivoRequest request) {
            registrarChamado = true;
            matriculaRecebida = matricula;
            return new DispositivoNotificacaoResponse(
                    "device-1",
                    "ANDROID",
                    true,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now());
        }

        @Override
        public boolean desativar(String matricula, String identificador) {
            desativarChamado = true;
            matriculaRecebida = matricula;
            identificadorRecebido = identificador;
            return true;
        }
    }
}

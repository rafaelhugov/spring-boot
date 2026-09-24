package mppb.restapi.mobile.feature.designacao.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoConsultaService;

class DesignacaoControllerTest {

    private FakeAuthService authService;
    private FakeConsultaService consultaService;
    private DesignacaoController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authService = new FakeAuthService();
        consultaService = new FakeConsultaService();
        controller = new DesignacaoController(authService, consultaService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ativasUsaLoginDoSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new TokenAuthentication(
                "token-auth",
                new UsuarioAutorizadoResponseDto(1L, "Usuario Teste", "usuario.teste"),
                null,
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")));

        ResponseEntity<DesignacoesAtivasResponse> response = controller.ativas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("usuario.teste", authService.loginRecebido);
        assertEquals("12345", consultaService.matriculaRecebida);
        assertTrue(consultaService.listarChamado);
    }

    @Test
    void ativasSemSecurityContextRetornaUnauthorized() {
        ResponseEntity<DesignacoesAtivasResponse> response = controller.ativas();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(consultaService.listarChamado);
    }

    @Test
    void ativasSemMatriculaRetornaUnauthorized() {
        authService.matricula = null;
        SecurityContextHolder.getContext().setAuthentication(new TokenAuthentication(
                "token-auth",
                new UsuarioAutorizadoResponseDto(1L, "Usuario Teste", "usuario.teste"),
                null,
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")));

        ResponseEntity<DesignacoesAtivasResponse> response = controller.ativas();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(consultaService.listarChamado);
    }

    private static class FakeAuthService extends DesignacaoAuthService {

        private String matricula = "12345";
        private String loginRecebido;

        FakeAuthService() {
            super(null, null);
        }

        @Override
        public String matriculaPorLogin(String login) {
            loginRecebido = login;
            return matricula;
        }
    }

    private static class FakeConsultaService extends DesignacaoConsultaService {

        private boolean listarChamado;
        private String matriculaRecebida;

        FakeConsultaService() {
            super(null, null, null);
        }

        @Override
        public DesignacoesAtivasResponse listarAtivas(String matricula) {
            listarChamado = true;
            matriculaRecebida = matricula;
            return new DesignacoesAtivasResponse("MEMBRO", List.of());
        }
    }
}

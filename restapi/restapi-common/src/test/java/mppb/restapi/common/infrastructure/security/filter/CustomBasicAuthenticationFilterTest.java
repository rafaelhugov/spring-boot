package mppb.restapi.common.infrastructure.security.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.manager.CustomAuthenticationManager;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomBasicAuthenticationFilterTest {

    private final CustomBasicAuthenticationFilter filter =
            new CustomBasicAuthenticationFilter(null);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveIgnorarRotasPublicasMesmoComPrefixoApi() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/cursos");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void deveFiltrarRotasPrivadas() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard/stats");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void deveAutenticarAuthorizationBearer() throws ServletException, IOException {
        CapturingProvider provider = new CapturingProvider();
        CustomBasicAuthenticationFilter filtroComProvider = new CustomBasicAuthenticationFilter(
                new CustomAuthenticationManager(List.of(provider)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/designacoes/ativas");
        request.addHeader("Authorization", "Bearer token-bearer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtroComProvider.doFilter(request, response, new MockFilterChain());

        assertEquals("token-bearer", provider.tokenRecebido);
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    @Test
    void deveManterCompatibilidadeComAuthorizationToken() throws ServletException, IOException {
        CapturingProvider provider = new CapturingProvider();
        CustomBasicAuthenticationFilter filtroComProvider = new CustomBasicAuthenticationFilter(
                new CustomAuthenticationManager(List.of(provider)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/designacoes/ativas");
        request.addHeader("Authorization", "Token token-legado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtroComProvider.doFilter(request, response, new MockFilterChain());

        assertEquals("token-legado", provider.tokenRecebido);
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    @Test
    void deveResponderUnauthorizedSemDetalheInternoQuandoTokenInvalido() throws ServletException, IOException {
        CapturingProvider provider = new CapturingProvider();
        provider.rejeitar = true;
        CustomBasicAuthenticationFilter filtroComProvider = new CustomBasicAuthenticationFilter(
                new CustomAuthenticationManager(List.of(provider)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/designacoes/ativas");
        request.addHeader("Authorization", "Bearer token-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtroComProvider.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("{\"error\":\"Unauthorized\"}", response.getContentAsString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static class CapturingProvider implements AuthenticationProvider {

        private String tokenRecebido;
        private boolean rejeitar;

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            tokenRecebido = ((TokenAuthentication) authentication).getToken();
            if (rejeitar) {
                throw new BadCredentialsException("detalhe interno sensivel");
            }
            return new TokenAuthentication(
                    tokenRecebido,
                    new UsuarioAutorizadoResponseDto(1L, "Usuario Teste", "usuario.teste"),
                    null,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER"));
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return TokenAuthentication.class.equals(authentication);
        }
    }
}

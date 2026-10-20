package mppb.restapi.mobile.feature.processoconsulta.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitInterceptorTest {

    @Test
    void naoConfiaEmXForwardedForEnviadoPeloCliente() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2, 2, 2, 100);

        assertTrue(invoke(interceptor, "/api/public/v1/processos/consulta", "10.0.0.1", "198.51.100.1").allowed());
        assertTrue(invoke(interceptor, "/api/public/v1/processos/consulta", "10.0.0.1", "198.51.100.2").allowed());
        Invocation blocked = invoke(interceptor, "/api/public/v1/processos/consulta", "10.0.0.1", "198.51.100.3");

        assertFalse(blocked.allowed());
        assertEquals(429, blocked.response().getStatus());
        assertEquals("60", blocked.response().getHeader("Retry-After"));
    }

    @Test
    void limitaPesquisaDetalhesECaptchaEmEscoposIndependentes() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1, 1, 1, 100);

        assertTrue(invoke(interceptor, "/api/public/v1/processos/consulta", "10.0.0.1", null).allowed());
        assertFalse(invoke(interceptor, "/api/public/v1/processos/consulta", "10.0.0.1", null).allowed());
        assertTrue(invoke(interceptor, "/api/public/v1/processos/123/detalhes", "10.0.0.1", null).allowed());
        assertFalse(invoke(interceptor, "/api/public/v1/processos/123/detalhes", "10.0.0.1", null).allowed());
        assertTrue(invoke(interceptor, "/api/public/v1/captcha", "10.0.0.1", null).allowed());
        assertFalse(invoke(interceptor, "/api/public/v1/captcha", "10.0.0.1", null).allowed());
    }

    @Test
    void limitaMemoriaMesmoComMuitosEnderecosReais() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1, 1, 1, 3);

        for (int i = 0; i < 20; i++) {
            invoke(interceptor, "/api/public/v1/captcha", "10.0.0." + i, null);
        }

        assertEquals(3, interceptor.trackedClients());
    }

    private Invocation invoke(RateLimitInterceptor interceptor, String uri, String remoteAddress, String forwardedFor)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        return new Invocation(interceptor.preHandle(request, response, new Object()), response);
    }

    private record Invocation(boolean allowed, MockHttpServletResponse response) {
    }
}

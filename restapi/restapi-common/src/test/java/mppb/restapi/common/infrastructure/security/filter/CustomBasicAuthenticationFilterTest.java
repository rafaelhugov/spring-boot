package mppb.restapi.common.infrastructure.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomBasicAuthenticationFilterTest {

    private final CustomBasicAuthenticationFilter filter =
            new CustomBasicAuthenticationFilter(null);

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
}

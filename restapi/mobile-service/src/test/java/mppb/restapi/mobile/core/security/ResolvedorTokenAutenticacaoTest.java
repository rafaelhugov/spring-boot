package mppb.restapi.mobile.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ResolvedorTokenAutenticacaoTest {

    @Test
    void bearerTemPrioridadeSobreTokenLegado() {
        String token = ResolvedorTokenAutenticacao.resolver("Bearer token-header", "token-query");

        assertEquals("token-header", token);
    }

    @Test
    void tokenHeaderLegadoContinuaAceitoDuranteTransicao() {
        String token = ResolvedorTokenAutenticacao.resolver("Token token-header", null);

        assertEquals("token-header", token);
    }

    @Test
    void queryLegadoContinuaAceitoDuranteTransicao() {
        String token = ResolvedorTokenAutenticacao.resolver(null, " token-query ");

        assertEquals("token-query", token);
    }

    @Test
    void ausenteRetornaNull() {
        assertNull(ResolvedorTokenAutenticacao.resolver(null, null));
        assertNull(ResolvedorTokenAutenticacao.resolver("Bearer   ", " "));
    }
}

package mppb.restapi.mobile.core.security;

public final class ResolvedorTokenAutenticacao {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PREFIX = "Token ";

    private ResolvedorTokenAutenticacao() {
    }

    public static String resolver(String authorization, String tokenLegado) {
        String token = extrairHeader(authorization);
        if (token != null) {
            return token;
        }
        if (tokenLegado == null || tokenLegado.isBlank()) {
            return null;
        }
        return tokenLegado.trim();
    }

    private static String extrairHeader(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String valor = authorization.trim();
        if (valor.startsWith(BEARER_PREFIX)) {
            return normalizar(valor.substring(BEARER_PREFIX.length()));
        }
        if (valor.startsWith(TOKEN_PREFIX)) {
            return normalizar(valor.substring(TOKEN_PREFIX.length()));
        }
        return null;
    }

    private static String normalizar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token.trim();
    }
}

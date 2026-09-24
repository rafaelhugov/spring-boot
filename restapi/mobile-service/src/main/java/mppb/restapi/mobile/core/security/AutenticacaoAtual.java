package mppb.restapi.mobile.core.security;

import java.util.Optional;

import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;

public final class AutenticacaoAtual {

    private AutenticacaoAtual() {
    }

    public static Optional<String> login() {
        return autenticacao()
                .map(TokenAuthentication::getUsuario)
                .map(usuario -> usuario == null ? null : usuario.getLogin())
                .filter(login -> !login.isBlank());
    }

    public static Optional<String> token() {
        return autenticacao()
                .map(TokenAuthentication::getToken)
                .filter(token -> token != null && !token.isBlank());
    }

    private static Optional<TokenAuthentication> autenticacao() {
        return SecurityUtils.getAuthenticationToken();
    }
}

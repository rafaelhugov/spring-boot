package mppb.restapi.common.infrastructure.security.util;


import java.util.*;

import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


public class SecurityUtils {

    public static Collection<? extends GrantedAuthority> getAuthorities(AcessoDataResponseDto acessoData) {
        if (acessoData == null || acessoData.getFuncionalidadesPermitidas() == null) {
            return Collections.emptySet();
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        acessoData.getFuncionalidadesPermitidas().stream().forEach(f -> authorities.add(new SimpleGrantedAuthority(f.getServico())));

        return authorities;
    }

    /**
     * Obtém o objeto TokenAuthentication do utilizador logado.
     * @return Um Optional contendo o TokenAuthentication.
     */
    public static Optional<TokenAuthentication> getAuthenticationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TokenAuthentication) {
            return Optional.of((TokenAuthentication) authentication);
        }
        return Optional.empty();
    }

}

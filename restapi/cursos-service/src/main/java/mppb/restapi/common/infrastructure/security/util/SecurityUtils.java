package mppb.restapi.common.infrastructure.security.util;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


public class SecurityUtils {

    public static Collection<? extends GrantedAuthority> getAuthorities(AcessoDataResponseDto acessoData) {
        if (acessoData.getFuncionalidadesPermitidas() == null) {
            return null;
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        acessoData.getFuncionalidadesPermitidas().stream().forEach(f -> authorities.add(new SimpleGrantedAuthority(f.getServico())));

        return authorities;
    }

}

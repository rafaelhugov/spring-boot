package mppb.microservices.common.infrastructure.security.util;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import mppb.microservices.common.client.authapi.dto.AcessoDataResponseTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


public class SecurityUtils {

    public static Collection<? extends GrantedAuthority> getAuthorities(AcessoDataResponseTO acessoData) {
        if (acessoData.getFuncionalidadesPermitidas() == null) {
            return null;
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        acessoData.getFuncionalidadesPermitidas().stream().forEach(f -> authorities.add(new SimpleGrantedAuthority(f.getServico())));

        return authorities;
    }

}

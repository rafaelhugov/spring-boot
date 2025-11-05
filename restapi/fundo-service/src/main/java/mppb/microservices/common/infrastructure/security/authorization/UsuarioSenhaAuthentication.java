package mppb.microservices.common.infrastructure.security.authorization;


import java.util.Collection;

import mppb.microservices.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioSenhaAuthentication extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = -3424650112216387008L;

    private UsuarioAutorizadoResponseDto usuario;

    private AcessoDataResponseDto acessoData;

    public UsuarioSenhaAuthentication(UsuarioAutorizadoResponseDto usuario, AcessoDataResponseDto acessoData,
                                      Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(usuario.getLogin(), credentials, authorities);
        this.usuario = usuario;
        this.acessoData = acessoData;
    }

    public UsuarioSenhaAuthentication(String credentials) {
        super(null, credentials);
    }

}

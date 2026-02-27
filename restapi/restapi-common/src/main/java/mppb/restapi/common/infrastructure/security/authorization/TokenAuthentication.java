package mppb.restapi.common.infrastructure.security.authorization;

import java.util.Collection;

import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenAuthentication extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = -3424650112216387008L;

    private String token;

    private UsuarioAutorizadoResponseDto usuario;

    private AcessoDataResponseDto acessoData;

    public TokenAuthentication(String token, UsuarioAutorizadoResponseDto usuario, AcessoDataResponseDto acessoData,
                               Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(usuario.getLogin(), credentials, authorities);
        this.token = token;
        this.usuario = usuario;
        this.acessoData = acessoData;
    }

    public TokenAuthentication(String token) {
        super(null, null, null);
        this.token = token;
    }

}


package mppb.microservices.common.infrastructure.security.authorization;

import java.util.Collection;

import mppb.microservices.common.client.authapi.dto.AcessoDataResponseTO;
import mppb.microservices.common.client.authapi.dto.UsuarioAutorizadoResponseTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenAuthentication extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = -3424650112216387008L;

    private String token;

    private UsuarioAutorizadoResponseTO usuario;

    private AcessoDataResponseTO acessoData;

    public TokenAuthentication(String token, UsuarioAutorizadoResponseTO usuario, AcessoDataResponseTO acessoData,
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


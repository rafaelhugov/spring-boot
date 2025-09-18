package mppb.microservices.common.infrastructure.security.provider;



import mppb.microservices.common.client.authapi.AuthServiceClient;
import mppb.microservices.common.client.authapi.dto.AcessoDataResponseTO;
import mppb.microservices.common.client.authapi.dto.ConsultaAutorizacaoRequestTO;
import mppb.microservices.common.client.authapi.dto.ConsultaAutorizacaoResponseTO;
import mppb.microservices.common.client.authapi.dto.UsuarioAutorizadoResponseTO;
import mppb.microservices.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.microservices.common.infrastructure.security.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import feign.FeignException;

@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {

    private final AuthServiceClient authServiceClient;

    @Value("${sistema.sigla:MPVIRTUAL}")
    private String siglaSistema;

    public TokenAuthenticationProvider(final AuthServiceClient authServiceClient) {
        super();
        this.authServiceClient = authServiceClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = ((TokenAuthentication) authentication).getToken();

        if (token == null || token.trim().equals("")) {
            throw new BadCredentialsException("security.undefined-response");
        }

        try {
            ConsultaAutorizacaoResponseTO consultaAutorizacaoResponseTO = authServiceClient
                    .consultaAutorizacao(new ConsultaAutorizacaoRequestTO(token, siglaSistema));
            UsuarioAutorizadoResponseTO usuario = consultaAutorizacaoResponseTO.getUsuario();
            AcessoDataResponseTO acessoData = consultaAutorizacaoResponseTO.getAcessoData();

            return new TokenAuthentication(token, usuario, acessoData, null, SecurityUtils.getAuthorities(acessoData));
        } catch (FeignException exception) {
            throw new BadCredentialsException("security.invalid-response");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(TokenAuthentication.class);
    }

}

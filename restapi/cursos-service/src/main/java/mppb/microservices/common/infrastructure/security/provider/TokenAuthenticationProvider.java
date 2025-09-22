package mppb.microservices.common.infrastructure.security.provider;



import mppb.microservices.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.microservices.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.microservices.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
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
            ConsultaAutorizacaoResponseDto consultaAutorizacaoResponseTO = authServiceClient
                    .consultaAutorizacao(new ConsultaAutorizacaoRequestDto(token, siglaSistema));
            UsuarioAutorizadoResponseDto usuario = consultaAutorizacaoResponseTO.getUsuario();
            AcessoDataResponseDto acessoData = consultaAutorizacaoResponseTO.getAcessoData();

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

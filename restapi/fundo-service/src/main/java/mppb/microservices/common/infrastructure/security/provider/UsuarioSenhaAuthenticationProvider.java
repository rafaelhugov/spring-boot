package mppb.microservices.common.infrastructure.security.provider;



import mppb.microservices.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.microservices.common.infrastructure.client.authapi.dto.*;
import mppb.microservices.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.microservices.common.infrastructure.security.authorization.UsuarioSenhaAuthentication;
import mppb.microservices.common.infrastructure.security.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import feign.FeignException;

@Component
public class UsuarioSenhaAuthenticationProvider implements AuthenticationProvider {

    private final AuthServiceClient authServiceClient;

    @Value("${sistema.sigla:MPVIRTUAL}")
    private String siglaSistema;

    public UsuarioSenhaAuthenticationProvider(final AuthServiceClient authServiceClient) {
        super();
        this.authServiceClient = authServiceClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String credentials = (String) authentication.getCredentials();

        if (credentials == null || credentials .trim().equals("")) {
            throw new BadCredentialsException("security.undefined-credentials");
        }

        try {

            String login = credentials.split(":")[0];
            String senha = credentials.split(":")[1];

            AutorizaResponseDto autorizaResponseTo = authServiceClient
                    .autoriza(new AutorizaRequestDto(login, senha, siglaSistema));
            String token = autorizaResponseTo.getToken();

            ConsultaAutorizacaoResponseDto consultaAutorizacaoResponseTO = authServiceClient.consultaAutorizacao(new ConsultaAutorizacaoRequestDto(token, siglaSistema));

            UsuarioAutorizadoResponseDto usuario = consultaAutorizacaoResponseTO.getUsuario();
            AcessoDataResponseDto acessoData = consultaAutorizacaoResponseTO.getAcessoData();

            return new TokenAuthentication(token, usuario, acessoData, null, SecurityUtils.getAuthorities(acessoData));
        } catch (FeignException exception) {
            throw new BadCredentialsException("security.invalid-credentials");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsuarioSenhaAuthentication.class);
    }

}

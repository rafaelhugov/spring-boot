package mppb.restapi.common.infrastructure.security.provider;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import feign.FeignException;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;

@Component
@Slf4j
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

            log.info("[AUTH] Token validado para usuarioId: {}, permissoes: {}",
                    usuario != null ? usuario.getId() : null,
                    acessoData != null ? acessoData.getFuncionalidadesPermitidas().size() : 0);

            // Injeção dinâmica de permissões para o perfil Servidor
            if (acessoData != null && acessoData.getPerfis() != null) {
                boolean isServidor = acessoData.getPerfis().stream()
                        .anyMatch(p -> "Servidor do Ministério Público".equalsIgnoreCase(p.getNome()));
                
                if (isServidor) {
                    List<FuncionalidadeAutorizadaResponseDto> permitidas = acessoData.getFuncionalidadesPermitidas();
                    if (permitidas == null) {
                        permitidas = new ArrayList<>();
                        acessoData.setFuncionalidadesPermitidas(permitidas);
                    }

                    boolean hasDashboard = permitidas.stream().anyMatch(f -> "cursos.dashboard".equals(f.getServico()));
                    boolean hasSolicitar = permitidas.stream().anyMatch(f -> "cursos.solicitar".equals(f.getServico()));

                    if (!hasDashboard) {
                        permitidas.add(new FuncionalidadeAutorizadaResponseDto(null, "Dashboard", "cursos.dashboard"));
                    }
                    if (!hasSolicitar) {
                        permitidas.add(new FuncionalidadeAutorizadaResponseDto(null, "Solicitar", "cursos.solicitar"));
                    }
                }
            }

            return new TokenAuthentication(token, usuario, acessoData, null, SecurityUtils.getAuthorities(acessoData));
        } catch (FeignException exception) {
            log.error("[AUTH] Erro na consulta de autorização. status={}, tipo={}",
                    exception.status(),
                    exception.getClass().getSimpleName());
            throw new BadCredentialsException("security.invalid-response");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(TokenAuthentication.class);
    }

}

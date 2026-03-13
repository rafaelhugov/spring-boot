package mppb.restapi.common.infrastructure.security.provider;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.*;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.authorization.UsuarioSenhaAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import feign.FeignException;

import java.util.List;
import java.util.ArrayList;

@Component
@Slf4j
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

        if (credentials == null || credentials.trim().equals("")) {
            throw new BadCredentialsException("security.undefined-credentials");
        }

        try {

            String login = credentials.split(":")[0];
            String senha = credentials.split(":")[1];
            String token = null;

            // Mock Fallback para Desenvolvimento - Pulando chamada externa
            if ("admin_teste".equals(login)) {
                token = "mock-admin-token";
            } else if ("solicitante_teste".equals(login)) {
                token = "mock-solicitante-token";
            } else {
                AutorizaResponseDto autorizaResponseTo = authServiceClient
                        .autoriza(new AutorizaRequestDto(login, senha, siglaSistema));
                token = autorizaResponseTo.getToken();
            }

            UsuarioAutorizadoResponseDto usuario;
            AcessoDataResponseDto acessoData;

            if (token != null && token.startsWith("mock-")) {
                if ("mock-admin-token".equals(token)) {
                    usuario = new UsuarioAutorizadoResponseDto(999L, "Administrador de Teste", "admin_teste");
                    List<FuncionalidadeAutorizadaResponseDto> perms = new ArrayList<>();
                    perms.add(new FuncionalidadeAutorizadaResponseDto(1L, "Dashboard", "cursos.dashboard"));
                    perms.add(new FuncionalidadeAutorizadaResponseDto(2L, "Analisar", "cursos.analisar.solicitacoes"));
                    perms.add(new FuncionalidadeAutorizadaResponseDto(3L, "Manter", "cursos.manter.cursos"));
                    acessoData = new AcessoDataResponseDto(null, perms, null, null);
                } else {
                    usuario = new UsuarioAutorizadoResponseDto(998L, "Solicitante de Teste", "solicitante_teste");
                    List<FuncionalidadeAutorizadaResponseDto> perms = new ArrayList<>();
                    perms.add(new FuncionalidadeAutorizadaResponseDto(1L, "Dashboard", "cursos.dashboard"));
                    perms.add(new FuncionalidadeAutorizadaResponseDto(4L, "Solicitar", "cursos.solicitar"));
                    acessoData = new AcessoDataResponseDto(null, perms, null, null);
                }
            } else {
                ConsultaAutorizacaoResponseDto consultaAutorizacaoResponseTO = authServiceClient
                        .consultaAutorizacao(new ConsultaAutorizacaoRequestDto(token, siglaSistema));
                usuario = consultaAutorizacaoResponseTO.getUsuario();
                acessoData = consultaAutorizacaoResponseTO.getAcessoData();

                // Mock Data Fallback para usuários de teste conhecidos (se o serviço retornar
                // null ou vazio)
                if (usuario == null || usuario.getLogin() == null) {
                    if ("admin_teste".equals(login)) {
                        usuario = new UsuarioAutorizadoResponseDto(999L, "Administrador de Teste", "admin_teste");
                    } else if ("solicitante_teste".equals(login)) {
                        usuario = new UsuarioAutorizadoResponseDto(998L, "Solicitante de Teste", "solicitante_teste");
                    }
                }
            }

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

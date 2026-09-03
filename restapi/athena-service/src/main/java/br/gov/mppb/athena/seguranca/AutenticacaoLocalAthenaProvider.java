package br.gov.mppb.athena.seguranca;

import java.util.Arrays;
import java.util.List;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.authorization.UsuarioSenhaAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class AutenticacaoLocalAthenaProvider implements AuthenticationProvider {

    @Value("${spring.profiles.active:}")
    private String perfisAtivos;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String credenciais = (String) authentication.getCredentials();
        if (credenciais == null || contemPerfil("prod")) {
            return null;
        }

        String login = credenciais.split(":", 2)[0];
        if ("ana.institucional".equals(login)) {
            AcessoDataResponseDto acesso = acessoSemPermissoesAthena();
            UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(1000L, "Ana Institucional", login);
            return new TokenAuthentication("mock-athena-participante-token", usuario, acesso, credenciais, SecurityUtils.getAuthorities(acesso));
        }
        if (!"admin_teste".equals(login)) {
            return null;
        }

        AcessoDataResponseDto acesso = new AcessoDataResponseDto(
                List.of(),
                List.of(new FuncionalidadeAutorizadaResponseDto(1L, "Administrador Athena", PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)),
                List.of(),
                List.of()
        );
        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(999L, "Administrador Athena Local", login);
        return new TokenAuthentication("mock-athena-admin-token", usuario, acesso, credenciais, SecurityUtils.getAuthorities(acesso));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsuarioSenhaAuthentication.class);
    }

    private boolean contemPerfil(String perfil) {
        if (perfisAtivos == null || perfisAtivos.isBlank()) {
            return false;
        }
        return Arrays.stream(perfisAtivos.split(","))
                .map(String::trim)
                .anyMatch(perfil::equals);
    }

    private static AcessoDataResponseDto acessoSemPermissoesAthena() {
        return new AcessoDataResponseDto(List.of(), List.of(), List.of(), List.of());
    }
}

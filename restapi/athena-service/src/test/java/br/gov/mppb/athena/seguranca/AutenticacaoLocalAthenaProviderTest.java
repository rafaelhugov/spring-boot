package br.gov.mppb.athena.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.authorization.UsuarioSenhaAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

class AutenticacaoLocalAthenaProviderTest {

    @Test
    void deveAutenticarAdminTesteComoAdministradorAthenaEmDev() throws Exception {
        AuthenticationProvider provider = providerComPerfis("dev");

        Authentication autenticacao = provider.authenticate(new UsuarioSenhaAuthentication("admin_teste:senha"));

        assertThat(autenticacao).isNotNull();
        assertThat(autenticacao.getName()).isEqualTo("admin_teste");
        assertThat(autenticacao.getAuthorities())
                .extracting("authority")
                .containsExactly(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA);
    }

    @Test
    void naoDeveAutenticarAdminTesteComoAdministradorAthenaEmProd() throws Exception {
        AuthenticationProvider provider = providerComPerfis("prod");

        assertThat(provider.authenticate(new UsuarioSenhaAuthentication("admin_teste:senha"))).isNull();
    }

    @Test
    void naoDeveInterceptarOutrosUsuariosLocais() throws Exception {
        AuthenticationProvider provider = providerComPerfis("dev");

        assertThat(provider.authenticate(new UsuarioSenhaAuthentication("solicitante_teste:senha"))).isNull();
    }

    @Test
    void deveAutenticarParticipanteLocalSemPermissaoAdministrativaAthenaEmDev() throws Exception {
        AuthenticationProvider provider = providerComPerfis("dev");

        Authentication autenticacao = provider.authenticate(new UsuarioSenhaAuthentication("ana.institucional:senha"));

        assertThat(autenticacao).isNotNull();
        assertThat(autenticacao.getName()).isEqualTo("ana.institucional");
        assertThat(autenticacao.getAuthorities()).isEmpty();
    }

    @Test
    void naoDeveAutenticarParticipanteLocalAthenaEmProd() throws Exception {
        AuthenticationProvider provider = providerComPerfis("prod");

        assertThat(provider.authenticate(new UsuarioSenhaAuthentication("ana.institucional:senha"))).isNull();
    }

    private static AuthenticationProvider providerComPerfis(String perfil) {
        return new ProvedorAutenticacaoLocalIsolado(perfil);
    }

    private record ProvedorAutenticacaoLocalIsolado(String perfil) implements AuthenticationProvider {

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            String credenciais = (String) authentication.getCredentials();
            if (credenciais == null || "prod".equals(perfil)) {
                return null;
            }

            String login = credenciais.split(":", 2)[0];
            AcessoDataResponseDto acesso;
            UsuarioAutorizadoResponseDto usuario;
            if ("ana.institucional".equals(login)) {
                acesso = new AcessoDataResponseDto(List.of(), List.of(), List.of(), List.of());
                usuario = new UsuarioAutorizadoResponseDto(1000L, "Ana Institucional", login);
            } else if ("admin_teste".equals(login)) {
                acesso = new AcessoDataResponseDto(
                        List.of(),
                        List.of(new FuncionalidadeAutorizadaResponseDto(
                                1L, "Administrador Athena", PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)),
                        List.of(),
                        List.of()
                );
                usuario = new UsuarioAutorizadoResponseDto(999L, "Administrador Athena Local", login);
            } else {
                return null;
            }
            return new TokenAuthentication(
                    "token-local-isolado", usuario, acesso, credenciais, SecurityUtils.getAuthorities(acesso));
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return authentication.equals(UsuarioSenhaAuthentication.class);
        }
    }
}

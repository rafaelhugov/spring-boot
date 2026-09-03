package br.gov.mppb.athena.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import mppb.restapi.common.infrastructure.security.authorization.UsuarioSenhaAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class AutenticacaoLocalAthenaProviderTest {

    @Test
    void deveAutenticarAdminTesteComoAdministradorAthenaEmDev() throws Exception {
        AutenticacaoLocalAthenaProvider provider = providerComPerfis("dev");

        Authentication autenticacao = provider.authenticate(new UsuarioSenhaAuthentication("admin_teste:senha"));

        assertThat(autenticacao).isNotNull();
        assertThat(autenticacao.getName()).isEqualTo("admin_teste");
        assertThat(autenticacao.getAuthorities())
                .extracting("authority")
                .containsExactly(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA);
    }

    @Test
    void naoDeveAutenticarAdminTesteComoAdministradorAthenaEmProd() throws Exception {
        AutenticacaoLocalAthenaProvider provider = providerComPerfis("prod");

        assertThat(provider.authenticate(new UsuarioSenhaAuthentication("admin_teste:senha"))).isNull();
    }

    @Test
    void naoDeveInterceptarOutrosUsuariosLocais() throws Exception {
        AutenticacaoLocalAthenaProvider provider = providerComPerfis("dev");

        assertThat(provider.authenticate(new UsuarioSenhaAuthentication("solicitante_teste:senha"))).isNull();
    }

    @Test
    void deveAutenticarParticipanteLocalSemPermissaoAdministrativaAthenaEmDev() throws Exception {
        AutenticacaoLocalAthenaProvider provider = providerComPerfis("dev");

        Authentication autenticacao = provider.authenticate(new UsuarioSenhaAuthentication("ana.institucional:senha"));

        assertThat(autenticacao).isNotNull();
        assertThat(autenticacao.getName()).isEqualTo("ana.institucional");
        assertThat(autenticacao.getAuthorities()).isEmpty();
    }

    @Test
    void naoDeveAutenticarParticipanteLocalAthenaEmProd() throws Exception {
        AutenticacaoLocalAthenaProvider provider = providerComPerfis("prod");

        assertThat(provider.authenticate(new UsuarioSenhaAuthentication("ana.institucional:senha"))).isNull();
    }

    private static AutenticacaoLocalAthenaProvider providerComPerfis(String perfis) throws Exception {
        AutenticacaoLocalAthenaProvider provider = new AutenticacaoLocalAthenaProvider();
        Field campo = AutenticacaoLocalAthenaProvider.class.getDeclaredField("perfisAtivos");
        campo.setAccessible(true);
        campo.set(provider, perfis);
        return provider;
    }
}

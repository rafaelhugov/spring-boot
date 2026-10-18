package mppb.restapi.mobile.feature.biometria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.mobile.feature.biometria.dto.AtivarBiometriaRequest;
import mppb.restapi.mobile.feature.biometria.dto.AtivarBiometriaResponse;
import mppb.restapi.mobile.feature.biometria.dto.AutenticarBiometriaResponse;
import mppb.restapi.mobile.feature.biometria.model.CredencialBiometrica;
import mppb.restapi.mobile.feature.biometria.repository.CredencialBiometricaRepository;
import mppb.restapi.mobile.feature.biometria.security.BiometricCredentialCipher;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorStoreReview;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;

class BiometricCredentialServiceTest {

    @Test
    void emiteCredencialOpacaERevalidaTokenNoUso() {
        InMemoryRepository repository = new InMemoryRepository();
        BiometricCredentialCipher cipher = new BiometricCredentialCipher(Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        StubAuthServiceClient authClient = new StubAuthServiceClient("usuario.teste");

        BiometricCredentialService service = new BiometricCredentialService(
                repository, cipher, authClient, 30, "ANDROID");
        AtivarBiometriaResponse issued = service.ativar(
                "usuario.teste",
                "token-institucional",
                new AtivarBiometriaRequest("device-1", "ANDROID"));
        AutenticarBiometriaResponse authenticated = service.autenticar(issued.credencial());

        assertTrue(issued.credencial().startsWith("BIO1."));
        assertNotEquals(issued.credencial(), repository.stored.segredoHash());
        assertEquals(64, repository.stored.segredoHash().length());
        assertEquals("token-institucional", authenticated.token());
        assertEquals(10L, repository.usedId);
        assertEquals(1, authClient.consultas);
    }

    @Test
    void autenticaCredencialLocalDeUsuarioGooglePeloValidadorStoreReview() {
        autenticarUsuarioStoreReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
    }

    @Test
    void autenticaCredencialLocalDeUsuarioApplePeloValidadorStoreReview() {
        autenticarUsuarioStoreReview(IdentidadeSintetica.REVIEW_APPLE_SERVIDOR);
    }

    @Test
    void autenticaCredencialLocalDeUsuarioUatPeloValidadorSintetico() {
        InMemoryRepository repository = new InMemoryRepository();
        StubAuthServiceClient authClient = new StubAuthServiceClient("nao-deve-ser-consultado");
        String token = AutenticadorUsuarioSintetico.TOKEN_PREFIX + "token-valido";
        AutenticadorUsuarioSintetico autenticadorUat =
                new StubAutenticadorUat(token, Optional.of(IdentidadeSintetica.UAT_MEMBRO));

        BiometricCredentialService service = service(
                repository, authClient, null, autenticadorUat);
        AtivarBiometriaResponse issued = service.ativar(
                IdentidadeSintetica.UAT_MEMBRO.getLogin(),
                token,
                new AtivarBiometriaRequest("device-uat", "ANDROID"));

        assertEquals(token, service.autenticar(issued.credencial()).token());
        assertEquals(0, authClient.consultas);
    }

    @Test
    void tokenStoreReviewInvalidoNaoFazFallbackParaAuthInstitucional() {
        InMemoryRepository repository = new InMemoryRepository();
        StubAuthServiceClient authClient = new StubAuthServiceClient("membro.google");
        String token = AutenticadorStoreReview.REVIEW_TOKEN_PREFIX + "token-invalido";
        AutenticadorStoreReview autenticadorReview =
                new StubAutenticadorStoreReview(token, Optional.empty());

        BiometricCredentialService service = service(
                repository, authClient, autenticadorReview, null);
        AtivarBiometriaResponse issued = service.ativar(
                "membro.google",
                token,
                new AtivarBiometriaRequest("device-review", "ANDROID"));

        assertThrows(
                BiometricCredentialService.CredencialBiometricaInvalidaException.class,
                () -> service.autenticar(issued.credencial()));
        assertEquals(0, authClient.consultas);
        assertEquals(10L, repository.deactivatedId);
    }

    private void autenticarUsuarioStoreReview(IdentidadeSintetica identidade) {
        InMemoryRepository repository = new InMemoryRepository();
        StubAuthServiceClient authClient = new StubAuthServiceClient("nao-deve-ser-consultado");
        String token = AutenticadorStoreReview.REVIEW_TOKEN_PREFIX + identidade.getLogin();
        AutenticadorStoreReview autenticadorReview =
                new StubAutenticadorStoreReview(token, Optional.of(identidade));

        BiometricCredentialService service = service(
                repository, authClient, autenticadorReview, null);
        AtivarBiometriaResponse issued = service.ativar(
                identidade.getLogin(),
                token,
                new AtivarBiometriaRequest("device-review", "ANDROID"));

        assertEquals(token, service.autenticar(issued.credencial()).token());
        assertEquals(10L, repository.usedId);
        assertEquals(0, authClient.consultas);
    }

    private BiometricCredentialService service(
            InMemoryRepository repository,
            AuthServiceClient authClient,
            AutenticadorStoreReview autenticadorReview,
            AutenticadorUsuarioSintetico autenticadorUat) {
        BiometricCredentialCipher cipher = new BiometricCredentialCipher(Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        return new BiometricCredentialService(
                repository,
                cipher,
                authClient,
                autenticadorReview,
                autenticadorUat,
                30,
                "ANDROID");
    }

    private static ConsultaAutorizacaoResponseDto autorizacao(String login) {
        return new ConsultaAutorizacaoResponseDto(
                null,
                new UsuarioAutorizadoResponseDto(1L, "Usuario", login));
    }

    private static final class StubAuthServiceClient implements AuthServiceClient {
        private final String login;
        private int consultas;

        private StubAuthServiceClient(String login) {
            this.login = login;
        }

        @Override
        public AutorizaResponseDto autoriza(AutorizaRequestDto request) {
            return null;
        }

        @Override
        public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto request) {
            consultas++;
            return autorizacao(login);
        }
    }

    private static final class StubAutenticadorStoreReview extends AutenticadorStoreReview {
        private final String tokenEsperado;
        private final Optional<IdentidadeSintetica> identidade;

        private StubAutenticadorStoreReview(
                String tokenEsperado,
                Optional<IdentidadeSintetica> identidade) {
            super(null, null, null);
            this.tokenEsperado = tokenEsperado;
            this.identidade = identidade;
        }

        @Override
        public Optional<IdentidadeSintetica> validarTokenReview(String token) {
            return tokenEsperado.equals(token) ? identidade : Optional.empty();
        }
    }

    private static final class StubAutenticadorUat extends AutenticadorUsuarioSintetico {
        private final String tokenEsperado;
        private final Optional<IdentidadeSintetica> identidade;

        private StubAutenticadorUat(
                String tokenEsperado,
                Optional<IdentidadeSintetica> identidade) {
            super(null, null);
            this.tokenEsperado = tokenEsperado;
            this.identidade = identidade;
        }

        @Override
        public Optional<IdentidadeSintetica> validarToken(String token) {
            return tokenEsperado.equals(token) ? identidade : Optional.empty();
        }
    }

    private static final class InMemoryRepository extends CredencialBiometricaRepository {
        private CredencialBiometrica stored;
        private Long usedId;
        private Long deactivatedId;

        private InMemoryRepository() {
            super(null);
        }

        @Override
        public CredencialBiometrica salvar(
                String seletor,
                String segredoHash,
                String login,
                String dispositivoId,
                String plataforma,
                String tokenInstitucionalCifrado,
                LocalDateTime expiraEm) {
            stored = new CredencialBiometrica(
                    10L,
                    seletor,
                    segredoHash,
                    login,
                    dispositivoId,
                    plataforma,
                    tokenInstitucionalCifrado,
                    true,
                    expiraEm,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null);
            return stored;
        }

        @Override
        public Optional<CredencialBiometrica> findAtivaBySeletor(String seletor) {
            return stored != null && stored.seletor().equals(seletor)
                    ? Optional.of(stored)
                    : Optional.empty();
        }

        @Override
        public void registrarUso(Long id) {
            usedId = id;
        }

        @Override
        public void desativarPorId(Long id) {
            deactivatedId = id;
        }
    }
}

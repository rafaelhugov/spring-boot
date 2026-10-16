package mppb.restapi.mobile.feature.biometria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

class BiometricCredentialServiceTest {

    @Test
    void emiteCredencialOpacaERevalidaTokenNoUso() {
        InMemoryRepository repository = new InMemoryRepository();
        BiometricCredentialCipher cipher = new BiometricCredentialCipher(Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        AuthServiceClient authClient = new StubAuthServiceClient("usuario.teste");

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
    }

    private static ConsultaAutorizacaoResponseDto autorizacao(String login) {
        return new ConsultaAutorizacaoResponseDto(
                null,
                new UsuarioAutorizadoResponseDto(1L, "Usuario", login));
    }

    private static final class StubAuthServiceClient implements AuthServiceClient {
        private final String login;

        private StubAuthServiceClient(String login) {
            this.login = login;
        }

        @Override
        public AutorizaResponseDto autoriza(AutorizaRequestDto request) {
            return null;
        }

        @Override
        public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto request) {
            return autorizacao(login);
        }
    }

    private static final class InMemoryRepository extends CredencialBiometricaRepository {
        private CredencialBiometrica stored;
        private Long usedId;

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
    }
}

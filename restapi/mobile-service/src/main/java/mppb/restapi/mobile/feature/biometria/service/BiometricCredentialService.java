package mppb.restapi.mobile.feature.biometria.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import feign.FeignException;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.mobile.feature.biometria.dto.AtivarBiometriaRequest;
import mppb.restapi.mobile.feature.biometria.dto.AtivarBiometriaResponse;
import mppb.restapi.mobile.feature.biometria.dto.AutenticarBiometriaResponse;
import mppb.restapi.mobile.feature.biometria.model.CredencialBiometrica;
import mppb.restapi.mobile.feature.biometria.repository.CredencialBiometricaRepository;
import mppb.restapi.mobile.feature.biometria.security.BiometricCredentialCipher;

@Service
@ConditionalOnProperty(prefix = "biometria", name = "enabled", havingValue = "true")
public class BiometricCredentialService {

    private static final String PREFIX = "BIO1";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CredencialBiometricaRepository repository;
    private final BiometricCredentialCipher cipher;
    private final AuthServiceClient authServiceClient;
    private final int ttlDays;
    private final String siglaSistema;

    public BiometricCredentialService(
            CredencialBiometricaRepository repository,
            BiometricCredentialCipher cipher,
            AuthServiceClient authServiceClient,
            @Value("${biometria.credential.ttl-days:30}") int ttlDays,
            @Value("${sistema.sigla:ANDROID}") String siglaSistema) {
        if (ttlDays < 1 || ttlDays > 90) {
            throw new IllegalStateException("biometria.credential.ttl-days deve estar entre 1 e 90");
        }
        this.repository = repository;
        this.cipher = cipher;
        this.authServiceClient = authServiceClient;
        this.ttlDays = ttlDays;
        this.siglaSistema = siglaSistema;
    }

    @Transactional
    public AtivarBiometriaResponse ativar(
            String loginAutenticado,
            String tokenInstitucional,
            AtivarBiometriaRequest request) {
        String login = obrigatorio(loginAutenticado, "login");
        String token = obrigatorio(tokenInstitucional, "token");
        String dispositivoId = obrigatorio(request.dispositivoId(), "dispositivoId");
        String plataforma = obrigatorio(request.plataforma(), "plataforma").toUpperCase(Locale.ROOT);
        validarTokenInstitucional(token, login);

        String seletor = UUID.randomUUID().toString();
        String segredo = novoSegredo();
        LocalDateTime expiraEm = LocalDateTime.now().plusDays(ttlDays);
        repository.salvar(
                seletor,
                sha256(segredo),
                login,
                dispositivoId,
                plataforma,
                cipher.cifrar(token),
                expiraEm);
        return new AtivarBiometriaResponse(formatarCredencial(seletor, segredo), expiraEm);
    }

    @Transactional(noRollbackFor = CredencialBiometricaInvalidaException.class)
    public AutenticarBiometriaResponse autenticar(String credencialCompleta) {
        PartesCredencial partes = parsear(credencialCompleta);
        CredencialBiometrica credencial = repository.findAtivaBySeletor(partes.seletor())
                .orElseThrow(CredencialBiometricaInvalidaException::new);

        if (credencial.expiraEm() == null || !credencial.expiraEm().isAfter(LocalDateTime.now())) {
            repository.desativarPorId(credencial.id());
            throw new CredencialBiometricaInvalidaException();
        }
        if (!MessageDigest.isEqual(
                credencial.segredoHash().getBytes(StandardCharsets.US_ASCII),
                sha256(partes.segredo()).getBytes(StandardCharsets.US_ASCII))) {
            throw new CredencialBiometricaInvalidaException();
        }

        try {
            String token = cipher.decifrar(credencial.tokenInstitucionalCifrado());
            validarTokenInstitucional(token, credencial.login());
            repository.registrarUso(credencial.id());
            return new AutenticarBiometriaResponse(token, credencial.expiraEm());
        } catch (CredencialBiometricaInvalidaException | IllegalStateException e) {
            repository.desativarPorId(credencial.id());
            throw new CredencialBiometricaInvalidaException();
        }
    }

    @Transactional
    public boolean desativar(String login, String dispositivoId) {
        String dispositivo = obrigatorio(dispositivoId, "dispositivoId");
        if (dispositivo.length() > 64) {
            throw new IllegalArgumentException("dispositivoId invalido");
        }
        return repository.desativar(
                obrigatorio(login, "login"),
                dispositivo);
    }

    @Scheduled(cron = "${biometria.credential.cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void desativarExpiradas() {
        repository.desativarExpiradas();
    }

    private void validarTokenInstitucional(String token, String loginEsperado) {
        try {
            ConsultaAutorizacaoResponseDto response = authServiceClient.consultaAutorizacao(
                    new ConsultaAutorizacaoRequestDto(token, siglaSistema));
            String loginRetornado = response == null || response.getUsuario() == null
                    ? null
                    : response.getUsuario().getLogin();
            if (loginRetornado == null || !loginEsperado.equalsIgnoreCase(loginRetornado.trim())) {
                throw new CredencialBiometricaInvalidaException();
            }
        } catch (FeignException | NullPointerException e) {
            throw new CredencialBiometricaInvalidaException();
        }
    }

    private static PartesCredencial parsear(String credencial) {
        if (credencial == null) {
            throw new CredencialBiometricaInvalidaException();
        }
        String[] partes = credencial.trim().split("\\.", -1);
        if (partes.length != 3 || !PREFIX.equals(partes[0])
                || partes[1].isBlank() || partes[2].isBlank()) {
            throw new CredencialBiometricaInvalidaException();
        }
        try {
            UUID.fromString(partes[1]);
        } catch (IllegalArgumentException e) {
            throw new CredencialBiometricaInvalidaException();
        }
        return new PartesCredencial(partes[1], partes[2]);
    }

    private static String novoSegredo() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String formatarCredencial(String seletor, String segredo) {
        return PREFIX + "." + seletor + "." + segredo;
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) {
                result.append(String.format("%02x", valueByte));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    private static String obrigatorio(String value, String campo) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatorio");
        }
        return value.trim();
    }

    private record PartesCredencial(String seletor, String segredo) {
    }

    public static final class CredencialBiometricaInvalidaException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

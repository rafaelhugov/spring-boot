package mppb.restapi.mobile.feature.notificacao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispositivoFcmService {

    private final DispositivoFcmRepository repository;

    @Transactional
    public DispositivoNotificacaoResponse registrar(String matricula, RegistrarDispositivoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request obrigatorio");
        }
        String matriculaNormalizada = obrigatorio(matricula, "matricula");
        String tokenFcm = obrigatorio(request.tokenFcm(), "tokenFcm");
        String plataforma = plataformaValida(request.plataforma());
        String tokenHash = sha256(tokenFcm);
        String identificador = normalizarIdentificador(request.identificador());

        Optional<DispositivoFcm> existentePorToken = repository.findByTokenHash(tokenHash);
        if (existentePorToken.isPresent()) {
            DispositivoFcm atualizado = repository.atualizar(
                    existentePorToken.get().id(),
                    matriculaNormalizada,
                    tokenFcm,
                    tokenHash,
                    plataforma);
            log.info("Dispositivo FCM registrado/reativado: identificador={}, plataforma={}",
                    atualizado.identificador(), atualizado.plataforma());
            return toResponse(atualizado);
        }

        boolean identificadorPertenceOutroUsuario = false;
        if (identificador != null) {
            Optional<DispositivoFcm> existentePorIdentificador = repository.findByIdentificador(identificador);
            if (existentePorIdentificador.isPresent()
                    && matriculaNormalizada.equals(existentePorIdentificador.get().servidorMatricula())) {
                DispositivoFcm atualizado = repository.atualizar(
                        existentePorIdentificador.get().id(),
                        matriculaNormalizada,
                        tokenFcm,
                        tokenHash,
                        plataforma);
                log.info("Dispositivo FCM atualizado por identificador: identificador={}, plataforma={}",
                        atualizado.identificador(), atualizado.plataforma());
                return toResponse(atualizado);
            }
            identificadorPertenceOutroUsuario = existentePorIdentificador.isPresent();
        }

        DispositivoFcm novo = repository.inserir(
                identificador == null || identificadorPertenceOutroUsuario ? UUID.randomUUID().toString() : identificador,
                matriculaNormalizada,
                tokenFcm,
                tokenHash,
                plataforma);
        log.info("Dispositivo FCM registrado: identificador={}, plataforma={}",
                novo.identificador(), novo.plataforma());
        return toResponse(novo);
    }

    @Transactional
    public boolean desativar(String matricula, String identificador) {
        String matriculaNormalizada = obrigatorio(matricula, "matricula");
        String identificadorNormalizado = obrigatorio(identificador, "identificador");
        boolean desativou = repository.desativar(matriculaNormalizada, identificadorNormalizado);
        if (desativou) {
            log.info("Dispositivo FCM desativado: identificador={}", identificadorNormalizado);
        }
        return desativou;
    }

    private DispositivoNotificacaoResponse toResponse(DispositivoFcm dispositivo) {
        return new DispositivoNotificacaoResponse(
                dispositivo.identificador(),
                dispositivo.plataforma(),
                dispositivo.ativo(),
                dispositivo.dataCriacao(),
                dispositivo.dataUltimaAtualizacao(),
                dispositivo.dataUltimoUso());
    }

    private static String obrigatorio(String value, String campo) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s obrigatorio".formatted(campo));
        }
        return value.trim();
    }

    private static String plataformaValida(String plataforma) {
        String normalizada = obrigatorio(plataforma, "plataforma").toUpperCase(Locale.ROOT);
        if (!"ANDROID".equals(normalizada) && !"IOS".equals(normalizada)) {
            throw new IllegalArgumentException("plataforma invalida");
        }
        return normalizada;
    }

    private static String normalizarIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return null;
        }
        String normalizado = identificador.trim();
        if (normalizado.length() > 64) {
            throw new IllegalArgumentException("identificador invalido");
        }
        return normalizado;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}

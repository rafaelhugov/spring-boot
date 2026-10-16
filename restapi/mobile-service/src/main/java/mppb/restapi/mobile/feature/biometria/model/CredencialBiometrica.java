package mppb.restapi.mobile.feature.biometria.model;

import java.time.LocalDateTime;

public record CredencialBiometrica(
        Long id,
        String seletor,
        String segredoHash,
        String login,
        String dispositivoId,
        String plataforma,
        String tokenInstitucionalCifrado,
        boolean ativa,
        LocalDateTime expiraEm,
        LocalDateTime dataCriacao,
        LocalDateTime dataUltimaAtualizacao,
        LocalDateTime dataUltimoUso) {
}

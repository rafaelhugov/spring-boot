package mppb.restapi.mobile.feature.notificacao.model;

import java.time.LocalDateTime;

public record DispositivoFcm(
        Long id,
        String identificador,
        String servidorMatricula,
        String tokenFcm,
        String tokenFcmHash,
        String plataforma,
        boolean ativo,
        LocalDateTime dataCriacao,
        LocalDateTime dataUltimaAtualizacao,
        LocalDateTime dataUltimoUso) {
}

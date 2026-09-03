package mppb.restapi.mobile.feature.notificacao.dto;

import java.time.LocalDateTime;

public record DispositivoNotificacaoResponse(
        String identificador,
        String plataforma,
        boolean ativo,
        LocalDateTime dataCriacao,
        LocalDateTime dataUltimaAtualizacao,
        LocalDateTime dataUltimoUso) {
}

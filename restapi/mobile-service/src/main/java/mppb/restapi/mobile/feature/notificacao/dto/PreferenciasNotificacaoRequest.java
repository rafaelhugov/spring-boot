package mppb.restapi.mobile.feature.notificacao.dto;

import jakarta.validation.constraints.NotNull;

public record PreferenciasNotificacaoRequest(
        @NotNull Boolean noticias,
        @NotNull Boolean designacoes,
        @NotNull Boolean contracheques,
        @NotNull Boolean pagamentos,
        @NotNull Boolean afastamentos) {
}

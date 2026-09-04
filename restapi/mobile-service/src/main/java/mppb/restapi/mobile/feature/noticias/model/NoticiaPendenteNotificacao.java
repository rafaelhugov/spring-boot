package mppb.restapi.mobile.feature.noticias.model;

public record NoticiaPendenteNotificacao(
        Long id,
        String identificadorReserva,
        String chaveExterna,
        String statusNotificacao,
        int quantidadeTentativas) {
}

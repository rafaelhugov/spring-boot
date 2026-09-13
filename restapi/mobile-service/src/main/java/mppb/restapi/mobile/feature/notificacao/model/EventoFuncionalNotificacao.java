package mppb.restapi.mobile.feature.notificacao.model;

public record EventoFuncionalNotificacao(
        String chaveEvento,
        String matricula,
        String tipoEvento,
        String rota,
        String titulo,
        String corpo) {
}

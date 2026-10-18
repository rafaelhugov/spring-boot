package mppb.restapi.mobile.feature.notificacao.model;

public record PreferenciasNotificacao(
        boolean noticias,
        boolean designacoes,
        boolean contracheques,
        boolean pagamentos,
        boolean afastamentos) {

    public static PreferenciasNotificacao todasHabilitadas() {
        return new PreferenciasNotificacao(true, true, true, true, true);
    }
}

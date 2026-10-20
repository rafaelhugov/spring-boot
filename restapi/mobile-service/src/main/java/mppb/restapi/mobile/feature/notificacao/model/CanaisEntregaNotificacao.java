package mppb.restapi.mobile.feature.notificacao.model;

public record CanaisEntregaNotificacao(boolean push, boolean central) {
    public boolean algumAtivo() {
        return push || central;
    }
}

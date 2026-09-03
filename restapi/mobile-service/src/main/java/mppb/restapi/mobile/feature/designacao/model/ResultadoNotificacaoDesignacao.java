package mppb.restapi.mobile.feature.designacao.model;

public record ResultadoNotificacaoDesignacao(
        int dispositivosAtivos,
        int aceitos,
        int tokensInvalidos,
        int falhasTransitorias,
        int falhasConfiguracao,
        int falhasDesconhecidas,
        boolean fcmDesligado) {

    public static ResultadoNotificacaoDesignacao desligado() {
        return new ResultadoNotificacaoDesignacao(0, 0, 0, 0, 0, 0, true);
    }

    public static ResultadoNotificacaoDesignacao semDispositivos() {
        return new ResultadoNotificacaoDesignacao(0, 0, 0, 0, 0, 0, false);
    }

    public boolean algumEnvioAceito() {
        return aceitos > 0;
    }

    public boolean nenhumDispositivoAtivo() {
        return !fcmDesligado && dispositivosAtivos == 0;
    }

    public boolean todosTokensInvalidos() {
        return dispositivosAtivos > 0 && tokensInvalidos == dispositivosAtivos;
    }

    public boolean possuiFalhaTransitoria() {
        return falhasTransitorias > 0;
    }

    public boolean possuiFalhaConfiguracao() {
        return falhasConfiguracao > 0;
    }

    public boolean possuiFalhaDesconhecida() {
        return falhasDesconhecidas > 0;
    }
}

package mppb.restapi.mobile.feature.notificacao.model;

public record ResultadoEnvioFcm(
        StatusEnvioFcm status,
        String messageId) {

    public static ResultadoEnvioFcm aceito(String messageId) {
        return new ResultadoEnvioFcm(StatusEnvioFcm.ACEITO, messageId);
    }

    public static ResultadoEnvioFcm tokenInvalido() {
        return new ResultadoEnvioFcm(StatusEnvioFcm.TOKEN_INVALIDO, null);
    }

    public static ResultadoEnvioFcm falhaTransitoria() {
        return new ResultadoEnvioFcm(StatusEnvioFcm.FALHA_TRANSITORIA, null);
    }

    public static ResultadoEnvioFcm falhaConfiguracao() {
        return new ResultadoEnvioFcm(StatusEnvioFcm.FALHA_CONFIGURACAO, null);
    }

    public static ResultadoEnvioFcm falhaDesconhecida() {
        return new ResultadoEnvioFcm(StatusEnvioFcm.FALHA_DESCONHECIDA, null);
    }
}

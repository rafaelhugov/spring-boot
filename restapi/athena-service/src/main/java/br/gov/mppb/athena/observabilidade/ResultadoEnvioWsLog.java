package br.gov.mppb.athena.observabilidade;

public record ResultadoEnvioWsLog(
        boolean enviado,
        String motivo
) {

    public ResultadoEnvioWsLog {
        motivo = motivo == null ? "" : motivo.strip();
    }

    public static ResultadoEnvioWsLog enviadoComSucesso() {
        return new ResultadoEnvioWsLog(true, "EVENTO_ENVIADO");
    }

    public static ResultadoEnvioWsLog desabilitado() {
        return new ResultadoEnvioWsLog(false, "WSLOG_NAO_CONFIGURADO");
    }
}

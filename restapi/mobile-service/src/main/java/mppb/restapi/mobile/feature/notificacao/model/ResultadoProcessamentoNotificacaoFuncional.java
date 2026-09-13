package mppb.restapi.mobile.feature.notificacao.model;

public record ResultadoProcessamentoNotificacaoFuncional(
        Long id,
        String statusInicial,
        String statusFinal,
        Integer quantidadeTentativas,
        String mensagemErro,
        ResultadoEnvioFcm resultadoEnvio) {
}

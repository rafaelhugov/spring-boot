package mppb.restapi.mobile.feature.designacao.model;

public record ResultadoProcessamentoNotificacaoDesignacao(
        Long controleId,
        String statusAnterior,
        String statusFinal,
        int quantidadeTentativasAnterior,
        String mensagemErro,
        ResultadoNotificacaoDesignacao resultadoNotificacao) {
}

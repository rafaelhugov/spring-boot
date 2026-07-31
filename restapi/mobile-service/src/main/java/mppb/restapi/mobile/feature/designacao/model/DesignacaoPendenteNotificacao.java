package mppb.restapi.mobile.feature.designacao.model;

public record DesignacaoPendenteNotificacao(
        Long controleId,
        String servidorMatricula,
        DesignacaoAtivaRow designacao) {
}

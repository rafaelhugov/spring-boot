package mppb.restapi.mobile.feature.designacao.model;

public record DesignacaoControlePendenteNotificacao(
        Long id,
        String identificadorReserva,
        String servidorMatricula,
        String statusNotificacao,
        int quantidadeTentativas,
        DesignacaoAtivaRow designacao) {
}

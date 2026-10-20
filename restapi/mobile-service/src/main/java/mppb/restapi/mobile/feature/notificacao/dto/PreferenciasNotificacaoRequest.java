package mppb.restapi.mobile.feature.notificacao.dto;

public record PreferenciasNotificacaoRequest(
        Boolean pushNoticias,
        Boolean pushDesignacoes,
        Boolean pushContracheques,
        Boolean pushPagamentos,
        Boolean pushAfastamentos,
        Boolean centralNoticias,
        Boolean centralDesignacoes,
        Boolean centralContracheques,
        Boolean centralPagamentos,
        Boolean centralAfastamentos) {
}

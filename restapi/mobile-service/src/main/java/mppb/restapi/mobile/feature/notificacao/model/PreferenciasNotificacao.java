package mppb.restapi.mobile.feature.notificacao.model;

public record PreferenciasNotificacao(
        boolean pushNoticias,
        boolean pushDesignacoes,
        boolean pushContracheques,
        boolean pushPagamentos,
        boolean pushAfastamentos,
        boolean centralNoticias,
        boolean centralDesignacoes,
        boolean centralContracheques,
        boolean centralPagamentos,
        boolean centralAfastamentos) {

    public PreferenciasNotificacao(
            boolean noticias,
            boolean designacoes,
            boolean contracheques,
            boolean pagamentos,
            boolean afastamentos) {
        this(noticias, designacoes, contracheques, pagamentos, afastamentos,
                noticias, designacoes, contracheques, pagamentos, afastamentos);
    }

    public static PreferenciasNotificacao todasHabilitadas() {
        return new PreferenciasNotificacao(true, true, true, true, true, true, true, true, true, true);
    }

    public boolean noticias() { return pushNoticias || centralNoticias; }
    public boolean designacoes() { return pushDesignacoes || centralDesignacoes; }
    public boolean contracheques() { return pushContracheques || centralContracheques; }
    public boolean pagamentos() { return pushPagamentos || centralPagamentos; }
    public boolean afastamentos() { return pushAfastamentos || centralAfastamentos; }
}

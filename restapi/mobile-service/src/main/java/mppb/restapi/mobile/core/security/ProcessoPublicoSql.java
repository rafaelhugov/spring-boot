package mppb.restapi.mobile.core.security;

/**
 * Predicados SQL obrigatorios para qualquer leitura publica de processo.
 *
 * <p>A regra e deliberadamente fail-closed: se a referencia ao ultimo movimento
 * de sigilo estiver inconsistente, o processo nao e considerado publico.</p>
 */
public final class ProcessoPublicoSql {

    public static final long CODIGO_DESCLASSIFICACAO_SIGILO = 920360L;

    private ProcessoPublicoSql() {
    }

    public static String processoPublico(String aliasProcesso, String sufixoAlias) {
        return aliasProcesso + ".tipoprotocolo = 'P' "
                + "AND " + aliasProcesso + ".id_pasta IS NULL "
                + "AND " + aliasProcesso + ".dataexclusao IS NULL "
                + "AND " + semSigiloAtivo(aliasProcesso, sufixoAlias);
    }

    public static String semSigiloAtivo(String aliasProtocolo, String sufixoAlias) {
        String movimento = "pm_sigilo_" + sufixoAlias;
        String taxonomia = "mov_sigilo_" + sufixoAlias;
        return "(" + aliasProtocolo + ".id_ultimomovimentosigilosidade IS NULL "
                + "OR EXISTS (SELECT 1 "
                + "FROM protocolo.protocolomovimento " + movimento + " "
                + "JOIN taxonomia.movimentos " + taxonomia
                + " ON " + taxonomia + ".id_item = " + movimento + ".id_movimento "
                + "WHERE " + movimento + ".id = " + aliasProtocolo + ".id_ultimomovimentosigilosidade "
                + "AND " + taxonomia + ".cod_movimento = " + CODIGO_DESCLASSIFICACAO_SIGILO
                + "))";
    }
}

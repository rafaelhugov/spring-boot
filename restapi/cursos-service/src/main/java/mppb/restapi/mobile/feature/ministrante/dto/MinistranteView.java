package mppb.restapi.mobile.feature.ministrante.dto;

public class MinistranteView {

    /**
     * Define a visão para listagens. Apenas os campos essenciais serão serializados.
     */
    public interface Listagem {}

    /**
     * Define a visão para detalhes. Herda os campos da listagem e pode incluir campos adicionais.
     */
    public interface Detalhes extends Listagem {}
}

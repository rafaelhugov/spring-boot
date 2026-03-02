package mppb.restapi.mobile.feature.participante.dto;

/**
 * Interface para controle de serialização JSON (JsonView) para a entidade Participante e seus DTOs.
 */
public interface ParticipanteView {

    /**
     * View para dados exibidos em listagens e buscas.
     */
    interface Listagem {}

    /**
     * View para dados exibidos em telas de detalhes (ainda não utilizada, mas definida para consistência).
     */
    interface Detalhes extends Listagem {}
}

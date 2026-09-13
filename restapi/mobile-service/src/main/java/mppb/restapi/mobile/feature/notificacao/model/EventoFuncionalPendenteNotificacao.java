package mppb.restapi.mobile.feature.notificacao.model;

import mppb.restapi.mobile.feature.notificacao.repository.NotificacaoDominioControleRepository;

public record EventoFuncionalPendenteNotificacao(
        Long id,
        String identificadorReserva,
        String statusNotificacao,
        Integer quantidadeTentativas,
        EventoFuncionalNotificacao evento,
        NotificacaoDominioControleRepository controleRepository) {

    public EventoFuncionalPendenteNotificacao(
            Long id,
            String identificadorReserva,
            String statusNotificacao,
            Integer quantidadeTentativas,
            EventoFuncionalNotificacao evento) {
        this(id, identificadorReserva, statusNotificacao, quantidadeTentativas, evento, null);
    }

    public EventoFuncionalPendenteNotificacao comControleRepository(
            NotificacaoDominioControleRepository controleRepository) {
        return new EventoFuncionalPendenteNotificacao(
                id,
                identificadorReserva,
                statusNotificacao,
                quantidadeTentativas,
                evento,
                controleRepository);
    }
}

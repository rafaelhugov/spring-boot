package mppb.restapi.mobile.feature.mensagem.dto;

import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository.DestinatarioRh;

public record DestinatarioRhResponse(
        String matricula,
        String nome,
        String setorNome,
        String cargoNome,
        String tipoCargo) {

    public static DestinatarioRhResponse from(DestinatarioRh item) {
        return new DestinatarioRhResponse(
                item.matricula(),
                item.nome(),
                item.setorNome(),
                item.cargoNome(),
                item.tipoCargo());
    }
}


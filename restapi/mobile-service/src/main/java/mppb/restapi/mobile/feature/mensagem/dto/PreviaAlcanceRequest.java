package mppb.restapi.mobile.feature.mensagem.dto;

public record PreviaAlcanceRequest(
        mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio modalidade,
        CriteriosDestinatariosDto criterios) {
}


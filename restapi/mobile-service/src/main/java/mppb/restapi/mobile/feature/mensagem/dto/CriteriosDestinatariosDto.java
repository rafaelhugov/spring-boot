package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

public record CriteriosDestinatariosDto(
        List<String> tiposCargo,
        List<Long> orgaoIds,
        List<Long> setorIds,
        List<Long> cargoIds,
        List<Long> grupoPersonalizadoIds,
        List<String> matriculas) {
}


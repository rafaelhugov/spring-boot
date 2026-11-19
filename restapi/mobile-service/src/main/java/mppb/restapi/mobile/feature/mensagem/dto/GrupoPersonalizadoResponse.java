package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

public record GrupoPersonalizadoResponse(
        Long id,
        String nome,
        String descricao,
        boolean ativo,
        int quantidadeMembros,
        List<String> matriculas) {
}


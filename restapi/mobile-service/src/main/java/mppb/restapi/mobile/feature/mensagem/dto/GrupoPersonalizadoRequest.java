package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoPersonalizadoRequest(
        @NotBlank(message = "O nome do grupo é obrigatório.")
        @Size(max = 120, message = "O nome do grupo deve ter no máximo 120 caracteres.")
        String nome,
        @Size(max = 300, message = "A descrição deve ter no máximo 300 caracteres.")
        String descricao,
        List<String> matriculas) {
}


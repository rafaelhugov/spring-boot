package mppb.restapi.cursos.feature.solicitacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HabilitarEdicaoRequestDto {

    @NotBlank(message = "A descrição dos ajustes é obrigatória.")
    @Size(max = 1000, message = "A descrição dos ajustes deve ter no máximo 1000 caracteres.")
    private String descricaoAjustes;
}

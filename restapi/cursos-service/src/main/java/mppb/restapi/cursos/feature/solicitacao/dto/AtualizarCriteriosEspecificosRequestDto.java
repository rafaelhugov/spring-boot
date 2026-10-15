package mppb.restapi.cursos.feature.solicitacao.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtualizarCriteriosEspecificosRequestDto {

    @Size(max = 4000, message = "Os critérios específicos do público-alvo devem ter no máximo 4000 caracteres")
    private String criteriosEspecificosPublicoAlvo;
}

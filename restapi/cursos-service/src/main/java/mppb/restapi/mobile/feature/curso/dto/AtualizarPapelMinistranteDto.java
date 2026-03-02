package mppb.restapi.mobile.feature.curso.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO (Data Transfer Object) utilizado como corpo da requisição para atualizar
 * o papel de um ministrante em uma associação específica com um curso.
 */
@Data
public class AtualizarPapelMinistranteDto {

    /**
     * O ID do novo papel de ministrante que será atribuído à associação.
     * Não pode ser nulo.
     */
    @NotNull(message = "O ID do papel não pode ser nulo.")
    private Long papelId;

}


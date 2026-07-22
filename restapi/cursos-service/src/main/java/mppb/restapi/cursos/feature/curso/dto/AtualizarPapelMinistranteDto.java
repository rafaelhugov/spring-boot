package mppb.restapi.cursos.feature.curso.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO utilizado para atualizar os dados editáveis de um ministrante em uma
 * associação específica com um curso.
 */
@Data
public class AtualizarPapelMinistranteDto {

    @NotNull(message = "O ID do papel não pode ser nulo.")
    private Long papelId;

    private String email;

    private String telefone;

    private Long idFoto;

    private Long idCurriculo;

}

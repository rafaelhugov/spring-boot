package mppb.restapi.cursos.feature.cadastro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.*;
import mppb.restapi.common.core.validation.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class EntidadeTutoraDto {

    @NotNull(groups = Update.class, message = "ID da entidade tutora é obrigatório.")
    @Null(groups = Creation.class)
    private Long id;

    @NotBlank
    private String nome;
}
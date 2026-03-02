package mppb.restapi.mobile.feature.cadastro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.*;
import mppb.restapi.common.core.validation.groups.Create;
import mppb.restapi.common.core.validation.groups.Update;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class EntidadeTutoraDto {

    @NotNull(groups = Update.class)
    @Null(groups = Create.class)
    private Long id;

    @NotBlank
    private String nome;
}
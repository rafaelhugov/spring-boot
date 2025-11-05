package mppb.restapi.cursos.feature.ministrante.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.*;
import mppb.restapi.common.core.validation.groups.Create;
import mppb.restapi.common.core.validation.groups.Update;
import mppb.restapi.cursos.feature.ministrante.enums.TipoMinistranteEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinistranteDto {

    @JsonView(MinistranteView.Listagem.class)
    @NotNull(groups = {Update.class})
    @Null(groups = Create.class)
    private Long id;

    @JsonView(MinistranteView.Listagem.class)
    // A validação de @NotBlank será aplicada na service, pois o nome
    // é obrigatório para externos, mas vem do RH para internos.
    private String nome;

    @JsonView(MinistranteView.Detalhes.class)
    @Size(min = 11, max = 11, message = "CPF deve conter 11 dígitos.")
    // A validação de @NotBlank/NotNull será aplicada na service, pois o CPF
    // é obrigatório apenas para ministrantes do tipo EXTERNO.
    private String cpf;

    @JsonView(MinistranteView.Listagem.class)
    private String nomeOrgaoExterno;

    @JsonView(MinistranteView.Detalhes.class)
    // A validação de @NotNull será aplicada na service, pois o idServidor
    // é obrigatório apenas para ministrantes do tipo INTERNO.
    private Integer idServidor;

    @JsonView(MinistranteView.Listagem.class)
    @NotNull(message = "O tipo do ministrante (INTERNO ou EXTERNO) é obrigatório.")
    private TipoMinistranteEnum tipo;

    // Campo transient, populado pela service para ministrantes internos.
    @JsonView(MinistranteView.Listagem.class)
    private String cargo;
}

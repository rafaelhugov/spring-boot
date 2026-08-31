package mppb.restapi.mobile.feature.certidaoextrajudicial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CertidaoExtrajudicialRequestDto {

    @NotNull(message = "O tipo de documento é obrigatório.")
    @Pattern(regexp = "F|J", message = "O tipo de documento deve ser F ou J.")
    private String tipoDocumento;

    @NotBlank(message = "O documento é obrigatório.")
    @Size(min = 11, max = 14, message = "O documento deve ter entre 11 e 14 dígitos.")
    private String documento;

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 60, message = "O nome deve ter entre 3 e 60 caracteres.")
    private String nome;
}

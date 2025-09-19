package mppb.microservices.fundoapi.feature.cobranca.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AreaSearchRequestDto {

    @NotBlank
    private String termo;

    @Min(8)
    @Max(80)
    @NotBlank
    private String numero;

    @Pattern(regexp = "\\d{5}", message = "O código deve ser uma sequência de 5 dígitos.")
    private String codigo;

}

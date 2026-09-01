package mppb.restapi.mobile.feature.concessaofolga.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitacaoMarcacaoFeriasRequestDto {

    @NotBlank
    private String dataInicial;

    @NotNull
    @Min(1)
    private Integer numeroDias;
}

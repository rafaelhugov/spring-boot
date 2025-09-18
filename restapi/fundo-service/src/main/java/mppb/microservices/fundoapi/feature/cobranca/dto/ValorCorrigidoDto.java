package mppb.microservices.fundoapi.feature.cobranca.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ValorCorrigidoDto {
    private Double indiceSelic;
    private Double percentualSelic;
    private Double valorCorrigidoSelic;
    private Double percentualTaxa;
    private Double valorFinalCorrigido;
}

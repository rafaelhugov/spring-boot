package mppb.microservices.fundoapi.feature.client.bb.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbAlterarValorNominalRequestDto {
    private Long numeroConvenio;
    private String indicadorNovoValorNominal;
    private AlteracaoValor alteracaoValor;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AlteracaoValor {
        private Double novoValorNominal;
    }
}
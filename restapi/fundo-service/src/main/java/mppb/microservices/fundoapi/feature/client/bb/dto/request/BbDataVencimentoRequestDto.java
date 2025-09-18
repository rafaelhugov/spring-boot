package mppb.microservices.fundoapi.feature.client.bb.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbDataVencimentoRequestDto {
    private Long numeroConvenio;
    private String indicadorNovaDataVencimento;
    private AlteracaoData alteracaoData;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AlteracaoData {
        private String novaDataVencimento;
    }
}
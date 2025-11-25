package mppb.microservices.fundoapi.infrastructure.client.bb.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbAlteracaoBoletoResponseDto {
    private Long numeroContratoCobranca;
    private String dataAtualizacao;
    private String horarioAtualizacao;
}

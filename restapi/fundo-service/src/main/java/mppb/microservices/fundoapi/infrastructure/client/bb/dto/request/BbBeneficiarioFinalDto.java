package mppb.microservices.fundoapi.infrastructure.client.bb.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbBeneficiarioFinalDto {

    // (1 - Pessoa física; 2 - Pessoa Jurídica.)
    private String tipoInscricao;
    private String numeroInscricao;
    private String nome;

}

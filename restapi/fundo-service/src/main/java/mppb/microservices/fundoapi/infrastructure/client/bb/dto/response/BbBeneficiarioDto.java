package mppb.microservices.fundoapi.infrastructure.client.bb.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbBeneficiarioDto {
    private String agencia;
    private String contaCorrente;
    private Long tipoEndereco;
    private String logradouro;
    private String bairro;
    private String cidade;
    private Long  codigoCidade;
    private String uf;
    private String cep;
    private String indicadorComprovacao;


}

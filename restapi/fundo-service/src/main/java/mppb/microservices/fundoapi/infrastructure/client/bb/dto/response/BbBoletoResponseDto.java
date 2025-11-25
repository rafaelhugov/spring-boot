package mppb.microservices.fundoapi.infrastructure.client.bb.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbBoletoResponseDto {


    private BbBeneficiarioDto beneficiario;
    private BbQrCodeDto qrCode;
    private String numero;
    private Long numeroCarteira;
    private Long numeroVariacaoCarteira;
    private String codigoCliente;
    private String linhaDigitavel;
    private String codigoBarraNumerico;
    private Long numeroContratoCobranca;
    private String urlImagemBoleto;
    private String observacao;
}

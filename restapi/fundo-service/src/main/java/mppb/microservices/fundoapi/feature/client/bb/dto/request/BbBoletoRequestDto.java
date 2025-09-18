package mppb.microservices.fundoapi.feature.client.bb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbBoletoRequestDto {


    @NotNull
    private Long numeroConvenio;
    @NotNull
    private Long numeroCarteira;
    @NotNull
    private Long numeroVariacaoCarteira;
    @NotNull
    private Long codigoModalidade;
    @NotNull
    private String dataEmissao;
    @NotNull
    private String dataVencimento;
    @NotNull
    private Double valorOriginal;
    @NotNull
    private String codigoAceite;
    @NotNull
    private String codigoTipoTitulo;
    private String descricaoTipoTitulo;
    @NotNull
    private String indicadorPermissaoRecebimentoParcial;
    @NotNull
    private String numeroTituloBeneficiario;
    @NotNull
    private String numeroTituloCliente;
    //(S ou N - Código para informar se o boleto terá um QRCode Pix vinculado)
    private String indicadorPix;
    private String textoEnderecoEmail;
    private BbPagadorDto pagador;
    private BbBeneficiarioFinalDto beneficiarioFinal;

}

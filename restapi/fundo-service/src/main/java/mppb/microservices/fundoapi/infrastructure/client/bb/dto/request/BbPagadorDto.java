package mppb.microservices.fundoapi.infrastructure.client.bb.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BbPagadorDto {
    @NotNull
    private Long tipoInscricao;
    @NotNull
    private Long numeroInscricao;
    private String nome;
    private String endereço;
    private String cep;
    private String cidade;
    private String bairro;
    private String uf;
    private String telefone;
}

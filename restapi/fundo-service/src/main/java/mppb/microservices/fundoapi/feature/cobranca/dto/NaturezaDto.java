package mppb.microservices.fundoapi.feature.cobranca.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class NaturezaDto {
    private Long id;
    private String nome;
    private String tipo;
    private String tipoDescricao;
}

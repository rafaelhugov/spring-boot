package mppb.microservices.fundoapi.feature.cobranca.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BoletoDto {
    private Long id;
    private Double valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private Long ordem;
    private Boolean impresso;
    private String identificador;
}

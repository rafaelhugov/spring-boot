package br.gov.mppb.ncap.feature.formtemplate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcaoRespostaDto {
    private Long id;
    private String descricao;
    private String valor;
}

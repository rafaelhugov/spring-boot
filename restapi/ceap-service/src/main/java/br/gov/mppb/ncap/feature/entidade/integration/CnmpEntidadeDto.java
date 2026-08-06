package br.gov.mppb.ncap.feature.entidade.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CnmpEntidadeDto {
    private Long id;
    private String nomecnmp;
    private String tipoentidade;
    private Long idComarca;
    private String municipio;
    private Boolean ativa;
}

package br.gov.mppb.ncap.feature.formtemplate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateDto {
    private Long id;
    private String resolucao;
    private String tipoentidade;
    private Boolean ativo;
    private Instant criadoem;
    private List<SecaoDto> secoes;
}

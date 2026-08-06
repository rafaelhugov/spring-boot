package br.gov.mppb.ncap.feature.formtemplate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecaoDto {
    private Long id;
    private String nome;
    private Integer ordem;
    private List<BlocoDto> blocos;

    // Customização de formulário NCAP
    private Boolean visible;
    private Boolean editable;
}

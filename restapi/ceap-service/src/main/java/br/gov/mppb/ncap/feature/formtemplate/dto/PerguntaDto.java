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
public class PerguntaDto {
    private Long id;
    private String idcnmp;
    private String label;
    private String campotipo;
    private Boolean obrigatorio;
    private Boolean exportarcnmp;
    private Integer ordem;
    private Integer tabulacao;
    private Long dependenciapaiId;
    private String dependenciavalor;
    private List<OpcaoRespostaDto> opcoes;

    // Customização de formulário NCAP
    private Boolean visible;
    private Boolean editable;
    private String campoCustomizado;
    private String preFillValue;
}

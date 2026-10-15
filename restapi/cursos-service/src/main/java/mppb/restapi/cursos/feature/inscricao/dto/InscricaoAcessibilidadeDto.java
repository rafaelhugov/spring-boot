package mppb.restapi.cursos.feature.inscricao.dto;

import lombok.Data;

@Data
public class InscricaoAcessibilidadeDto {

    private Boolean pcd;
    private String necessidadeEspecial;
    private Boolean declaracaoCriteriosPublicoAlvo;
}

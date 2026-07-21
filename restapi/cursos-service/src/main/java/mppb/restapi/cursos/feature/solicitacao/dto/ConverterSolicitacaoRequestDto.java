package mppb.restapi.cursos.feature.solicitacao.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConverterSolicitacaoRequestDto {
    private List<Long> municipioIds;
}

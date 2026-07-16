package mppb.restapi.cursos.feature.frequencia.dto;

import lombok.Data;
import java.util.List;

@Data
public class AtualizarFrequenciasManuaisDto {
    private List<RegistroPresencaManualDto> presencas;
}

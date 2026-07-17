package mppb.restapi.cursos.feature.frequencia.dto;

import lombok.Data;
import java.util.List;

@Data
public class RegistroPresencaManualDto {
    private Long aulaId;
    private List<String> horarios;
}

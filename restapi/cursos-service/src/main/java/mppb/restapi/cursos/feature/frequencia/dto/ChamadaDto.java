package mppb.restapi.cursos.feature.frequencia.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChamadaDto {
    private Long id;
    private String token;
    private LocalDateTime dataGeracao;
}
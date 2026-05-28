package mppb.restapi.cursos.feature.frequencia.dto;

import lombok.Data;
import mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum;

import java.time.LocalDateTime;

@Data
public class ChamadaDto {
    private Long id;
    private String token;
    private LocalDateTime dataGeracao;
    private TipoChamadaEnum tipo;
    private boolean aberta;
}
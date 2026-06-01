package mppb.restapi.cursos.feature.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long minhasInscricoes;
    private long cursosConcluidos;
    private long cursosEmAndamento;
    private long certificadosDisponiveis;
    private long horasCapacitacao;
    private long aguardandoInicio;
    private long taxaConclusao;
    private String proximoCurso;
    private long pendencias;
    private List<ProximaAcaoDto> proximasAcoes;
}

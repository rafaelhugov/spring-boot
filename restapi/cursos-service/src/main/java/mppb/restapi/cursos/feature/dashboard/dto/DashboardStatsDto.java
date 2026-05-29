package mppb.restapi.cursos.feature.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long cursosAbertos;
    private long cursosEmAndamento;
    private long vagasDisponiveis;
}

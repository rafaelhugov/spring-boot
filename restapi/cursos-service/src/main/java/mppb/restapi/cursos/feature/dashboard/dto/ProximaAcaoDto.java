package mppb.restapi.cursos.feature.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProximaAcaoDto {
    private String titulo;
    private String descricao;
    private String tipo; // Ex: INFO, WARNING, SUCCESS
}

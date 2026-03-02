package mppb.restapi.cursos.feature.lookup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalEventoDto {
    private Long id;
    private String nomeLocal;
}

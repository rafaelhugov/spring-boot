package mppb.restapi.cursos.feature.curso.dto;

import lombok.*;
import mppb.restapi.cursos.feature.lookup.dto.PapelMinistranteDto;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CursoMinistranteDto {

    private Long id; // ID da associação

    private MinistranteDto ministrante;

    private PapelMinistranteDto papel;

}

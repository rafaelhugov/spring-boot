package mppb.restapi.mobile.feature.curso.dto;

import lombok.*;
import mppb.restapi.mobile.feature.lookup.dto.PapelMinistranteDto;
import mppb.restapi.mobile.feature.ministrante.dto.MinistranteDto;

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

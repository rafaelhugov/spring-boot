package mppb.restapi.cursos.feature.lookup.dto;

import lombok.*;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PapelMinistranteDto {
    private Long id;
    private String nome;
}
package mppb.restapi.cursos.feature.lookup.dto;

import lombok.*;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TipoEventoDto {
    private Long id;
    private String descricao;
}
package mppb.restapi.cursos.feature.lookup.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import mppb.restapi.cursos.feature.curso.dto.CursoView;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ModalidadeDto {

    @JsonView(CursoView.Listagem.class)
    private Long id;

    @JsonView(CursoView.Listagem.class)
    private String descricao;
}
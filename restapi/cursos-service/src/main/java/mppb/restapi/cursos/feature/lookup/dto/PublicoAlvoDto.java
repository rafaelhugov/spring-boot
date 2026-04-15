package mppb.restapi.cursos.feature.lookup.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import mppb.restapi.cursos.feature.curso.dto.CursoView;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PublicoAlvoDto {

    @JsonView(CursoView.Listagem.class)
    private Long id;

    @JsonView(CursoView.Listagem.class)
    private String nome;

    @JsonView(CursoView.Listagem.class)
    private String internoExterno;

    public String getDescricao() {
        return nome;
    }
}
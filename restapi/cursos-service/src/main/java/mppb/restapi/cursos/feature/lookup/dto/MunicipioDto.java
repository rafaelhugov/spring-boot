package mppb.restapi.cursos.feature.lookup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonView;
import mppb.restapi.cursos.feature.curso.dto.CursoView;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MunicipioDto {
    
    @JsonView(CursoView.Listagem.class)
    private Long id;
    
    @JsonView(CursoView.Listagem.class)
    private String nome;
}

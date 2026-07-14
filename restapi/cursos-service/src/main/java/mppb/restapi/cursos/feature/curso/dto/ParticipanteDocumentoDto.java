package mppb.restapi.cursos.feature.curso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteDocumentoDto {
    private String nome;
    private String cargo;
    private String matricula;
    private String tipo;
}

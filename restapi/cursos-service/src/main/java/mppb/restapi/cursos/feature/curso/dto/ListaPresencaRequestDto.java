package mppb.restapi.cursos.feature.curso.dto;

import lombok.Data;

@Data
public class ListaPresencaRequestDto {
    private String tipoPublico;      // MEMBROS, SERVIDORES, ASSESSORES, TODOS
    private String nomeCurso;
    private String localEvento;
    private String dataEvento;
    private String horarioEvento;
}

package mppb.restapi.cursos.feature.common.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ServidorDto {
    private String tipoCargoNome;
    private Integer id;
    private String matricula;
    private String nome;
    private String cargo;
}

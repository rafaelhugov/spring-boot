package mppb.restapi.cursos.feature.cadastro.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class EntidadeTutoraDto {
    private Long id;
    private String nome;
}
package mppb.restapi.cursos.feature.lookup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ModalidadeDto {
    private Long id;
    private String descricao;
}
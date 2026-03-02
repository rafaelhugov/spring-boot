package mppb.restapi.cursos.feature.lookup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SituacaoInscricaoDto {
    private Character codigo;
    private String descricao;
}

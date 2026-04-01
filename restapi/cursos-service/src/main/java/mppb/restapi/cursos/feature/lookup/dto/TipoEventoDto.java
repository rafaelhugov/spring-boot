package mppb.restapi.cursos.feature.lookup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TipoEventoDto {
    private Long id;
    private String descricao;

    public String getNome() {
        return descricao;
    }
}
package mppb.restapi.mobile.feature.lookup.dto;

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
}
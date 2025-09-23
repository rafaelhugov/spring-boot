package mppb.microservices.cursosapi.feature.lookup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PapelMinistranteDto {
    private Long id;
    private String nome;
}
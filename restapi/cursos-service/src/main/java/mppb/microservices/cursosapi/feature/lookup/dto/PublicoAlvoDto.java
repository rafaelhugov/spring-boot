package mppb.microservices.cursosapi.feature.lookup.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PublicoAlvoDto {
    private Long id;
    private String nome;
}
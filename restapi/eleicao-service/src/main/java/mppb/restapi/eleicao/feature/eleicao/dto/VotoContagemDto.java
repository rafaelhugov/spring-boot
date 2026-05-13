package mppb.restapi.eleicao.feature.eleicao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VotoContagemDto {
    private Long idCandidato;
    private String nomeCandidato;
    private String tituloCandidato;
    private Long idFoto;
    private Long quantidadeVotos;
    private Double percentualTodos;
    private Double percentualValidos;
}

package mppb.restapi.eleicao.feature.eleicao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoDto {
    private Long idEleicao;
    private String tituloEleicao;
    private Long totalEleitoresAptos;
    private Long totalVotosComputados;
    private Long totalVotosValidos;
    private Long totalVotosBrancos;
    private Long totalVotosNulos;
    private Long totalVotosComputaveis;
    
    private List<VotoContagemDto> candidatos = new ArrayList<>();
}

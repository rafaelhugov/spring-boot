package mppb.restapi.eleicao.feature.voto.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.eleicao.feature.candidato.dto.CandidatoDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VotoDto {
    private Long id;
    private Long idEleicao;
    private List<Long> candidatosIds;
    private List<CandidatoDto> candidatos;
    private String tipo;
    private String hash;
    private String ip;
}

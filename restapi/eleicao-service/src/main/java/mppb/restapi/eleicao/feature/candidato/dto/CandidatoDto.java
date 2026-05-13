package mppb.restapi.eleicao.feature.candidato.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CandidatoDto {
    private Long id;
    private Long idEleicao;
    private Long idServidor;
    private Long idFoto;
    private String titulo;
    private String nome;
    private Integer ordem;
    private Boolean ativo;
}

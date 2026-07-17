package mppb.restapi.cursos.feature.inscricao.dto;

import lombok.Data;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;

import java.time.LocalDateTime;

/**
 * DTO para representar os dados de uma inscrição na resposta da API.
 */
@Data
public class InscricaoDto {

    private Long id;
    private mppb.restapi.cursos.feature.curso.dto.CursoDto curso;
    private LocalDateTime dataInscricao;
    private String situacao;
    private Integer frequencia;
    private Boolean pcd;
    private String necessidadeEspecial;
    private ParticipanteDto participante;
    private Boolean possuiFrequenciaManual;

}

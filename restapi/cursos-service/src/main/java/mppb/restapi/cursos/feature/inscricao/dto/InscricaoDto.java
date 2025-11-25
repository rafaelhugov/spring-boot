package mppb.restapi.cursos.feature.inscricao.dto;

import lombok.Data;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;

import java.time.LocalDateTime;

/**
 * DTO para representar os dados de uma inscrição na resposta da API.
 */
@Data
public class InscricaoDto {

    private Long id;
    private LocalDateTime dataInscricao;
    private String situacao;
    private Integer frequencia;
    private ParticipanteDto participante;

}

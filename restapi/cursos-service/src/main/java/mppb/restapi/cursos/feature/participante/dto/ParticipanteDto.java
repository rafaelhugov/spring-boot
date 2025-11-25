package mppb.restapi.cursos.feature.participante.dto;

import lombok.Data;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;

/**
 * DTO para representar os dados de um participante na resposta da API.
 */
@Data
public class ParticipanteDto {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private Integer idServidor;
    private TipoParticipanteEnum tipo;
    private String cargo;

}

package mppb.restapi.cursos.feature.participante.dto;

import lombok.*;

/**
 * DTO para a resposta da verificação de um participante por CPF.
 * Contém apenas o ID e o e-mail mascarado para segurança (LGPD).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ParticipanteVerificacaoDto {

    private Long participanteId;
    private String emailMascarado;
}

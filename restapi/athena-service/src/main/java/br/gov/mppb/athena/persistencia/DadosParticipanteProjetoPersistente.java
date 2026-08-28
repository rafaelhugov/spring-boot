package br.gov.mppb.athena.persistencia;

import java.util.UUID;

public record DadosParticipanteProjetoPersistente(
        UUID id,
        String identificador,
        String nome,
        String login,
        String unidade,
        String papel
) {
}

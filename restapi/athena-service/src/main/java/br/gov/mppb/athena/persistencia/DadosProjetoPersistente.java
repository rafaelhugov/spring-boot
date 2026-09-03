package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosProjetoPersistente(
        UUID id,
        String identificador,
        String nome,
        String descricao,
        String situacao,
        OffsetDateTime dataCriacao,
        UUID idUsuarioCriacao
) {
}

package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosVinculoProjetoPersistente(
        UUID id,
        UUID projetoId,
        UUID usuarioId,
        String papel,
        String unidade,
        OffsetDateTime inicioEm,
        UUID registradoPorId
) {
}

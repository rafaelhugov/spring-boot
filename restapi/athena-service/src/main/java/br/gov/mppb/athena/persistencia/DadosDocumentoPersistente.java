package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosDocumentoPersistente(
        UUID id,
        String identificador,
        UUID projetoId,
        String titulo,
        String grupo,
        String categoria,
        String finalidade,
        String apresentacao,
        OffsetDateTime criadoEm,
        UUID criadoPorId
) {
}

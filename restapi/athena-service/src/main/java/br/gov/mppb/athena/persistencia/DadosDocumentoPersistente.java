package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosDocumentoPersistente(
        UUID id,
        String identificador,
        UUID idProjeto,
        String titulo,
        String grupo,
        String categoria,
        String finalidade,
        String apresentacao,
        OffsetDateTime dataCriacao,
        UUID idUsuarioCriacao
) {
}

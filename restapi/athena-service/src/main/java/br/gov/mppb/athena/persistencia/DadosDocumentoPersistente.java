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
        UUID idUsuarioCriacao,
        UUID idVersaoAtual
) {
    public DadosDocumentoPersistente(
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
        this(id, identificador, idProjeto, titulo, grupo, categoria, finalidade, apresentacao,
                dataCriacao, idUsuarioCriacao, null);
    }
}

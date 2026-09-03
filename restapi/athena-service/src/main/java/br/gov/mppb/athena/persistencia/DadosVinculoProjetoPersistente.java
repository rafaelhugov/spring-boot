package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosVinculoProjetoPersistente(
        UUID id,
        UUID idProjeto,
        UUID idUsuario,
        String papel,
        String unidade,
        OffsetDateTime dataInicio,
        UUID idUsuarioRegistro
) {
}

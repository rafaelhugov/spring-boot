package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosPedidoAjustePersistente(
        UUID id,
        UUID itemId,
        UUID solicitanteId,
        String descricao,
        String justificativa,
        String situacao,
        OffsetDateTime solicitadoEm,
        String responsavelTecnico,
        OffsetDateTime tratadoEm,
        String decisaoTratamento,
        String justificativaTratamento,
        UUID versaoResultanteId
) {
}

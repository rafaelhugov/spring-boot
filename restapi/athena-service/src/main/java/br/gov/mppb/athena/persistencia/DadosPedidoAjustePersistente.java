package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosPedidoAjustePersistente(
        UUID id,
        UUID idItem,
        UUID idSolicitante,
        String descricao,
        String justificativa,
        String situacao,
        OffsetDateTime dataSolicitacao,
        String responsavelTecnico,
        OffsetDateTime dataTratamento,
        String decisaoTratamento,
        String justificativaTratamento,
        UUID idVersaoResultante
) {
}

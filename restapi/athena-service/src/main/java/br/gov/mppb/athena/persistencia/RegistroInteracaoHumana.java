package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistroInteracaoHumana(
        UUID id,
        UUID idItem,
        UUID idUsuario,
        TipoInteracaoHumana tipo,
        String decisao,
        String conteudo,
        String justificativa,
        OffsetDateTime dataRegistro,
        UUID eventoAuditoriaId
) {
}

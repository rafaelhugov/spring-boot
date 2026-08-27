package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosVersaoDocumentoPersistente(
        UUID id,
        UUID documentoId,
        int numero,
        String formato,
        String conteudoOriginal,
        String hashConteudo,
        OffsetDateTime recebidaEm,
        UUID recebidaPorId,
        String origemExterna
) {
}

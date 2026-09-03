package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosVersaoDocumentoPersistente(
        UUID id,
        UUID idDocumento,
        int numero,
        String formato,
        String conteudoOriginal,
        String hashConteudo,
        OffsetDateTime dataRecebimento,
        UUID idUsuarioRecebimento,
        String origemExterna,
        UUID idVersaoAnterior,
        boolean confirmada,
        UUID idPedidoAjusteOrigem
) {

    public DadosVersaoDocumentoPersistente(
            UUID id,
            UUID idDocumento,
            int numero,
            String formato,
            String conteudoOriginal,
            String hashConteudo,
            OffsetDateTime dataRecebimento,
            UUID idUsuarioRecebimento,
            String origemExterna
    ) {
        this(id, idDocumento, numero, formato, conteudoOriginal, hashConteudo, dataRecebimento, idUsuarioRecebimento,
                origemExterna, null, false, null);
    }
}

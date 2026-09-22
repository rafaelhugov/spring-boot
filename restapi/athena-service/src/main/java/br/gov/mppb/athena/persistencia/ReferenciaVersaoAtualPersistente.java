package br.gov.mppb.athena.persistencia;

import java.util.UUID;

public record ReferenciaVersaoAtualPersistente(
        UUID idVersaoAtual,
        long versaoControle
) {
}

package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.Documento;
import java.util.UUID;

public record ResultadoIngestaoPersistente(
        Documento documento,
        UUID idProjetoPersistente,
        UUID idDocumentoPersistente
) {
}

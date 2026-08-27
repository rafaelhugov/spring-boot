package br.gov.mppb.athena.persistencia;

import java.util.List;

public record HistoricoDocumentoPersistente(
        DadosProjetoPersistente projeto,
        DadosDocumentoPersistente documento,
        List<DadosVersaoDocumentoPersistente> versoes,
        List<DadosItemDocumentalPersistente> itens
) {
}

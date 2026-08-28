package br.gov.mppb.athena.persistencia;

import java.util.List;

public record HistoricoDocumentoPersistente(
        DadosProjetoPersistente projeto,
        DadosDocumentoPersistente documento,
        List<DadosVersaoDocumentoPersistente> versoes,
        List<DadosItemDocumentalPersistente> itens
) {

    public HistoricoDocumentoPersistente {
        versoes = versoes == null ? List.of() : List.copyOf(versoes);
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    @Override
    public List<DadosVersaoDocumentoPersistente> versoes() {
        return List.copyOf(versoes);
    }

    @Override
    public List<DadosItemDocumentalPersistente> itens() {
        return List.copyOf(itens);
    }
}

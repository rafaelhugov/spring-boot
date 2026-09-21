package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import java.util.List;
import java.util.Map;

public record ArquivoDocumentalValidado(
        byte[] original,
        String sha256,
        FormatoDocumento formato,
        Map<String, Object> metadados,
        List<Map<String, Object>> itens,
        String chaveIdempotencia
) {
    public ArquivoDocumentalValidado {
        original = original.clone();
        metadados = Map.copyOf(metadados);
        itens = itens.stream().map(Map::copyOf).toList();
    }

    @Override
    public byte[] original() {
        return original.clone();
    }
}

package br.gov.mppb.athena.integracao;

import java.util.List;
import java.util.Map;

public record DocumentoRuntimeInterpretado(
        String tipoContrato,
        String versaoContrato,
        Map<String, Object> metadados,
        List<ItemRuntimeInterpretado> itens
) {

    public DocumentoRuntimeInterpretado {
        metadados = metadados == null ? Map.of() : Map.copyOf(metadados);
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    @Override
    public Map<String, Object> metadados() {
        return Map.copyOf(metadados);
    }

    @Override
    public List<ItemRuntimeInterpretado> itens() {
        return List.copyOf(itens);
    }

    public boolean contratoRuntime() {
        return tipoContrato != null && !tipoContrato.isBlank();
    }
}

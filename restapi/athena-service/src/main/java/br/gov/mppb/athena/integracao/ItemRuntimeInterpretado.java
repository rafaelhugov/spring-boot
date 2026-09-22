package br.gov.mppb.athena.integracao;

import java.util.List;

public record ItemRuntimeInterpretado(
        String identificador,
        String titulo,
        String descricao,
        String categoria,
        String estadoConhecimento,
        String estadoGovernanca,
        Integer versaoItem,
        String hashConteudo,
        List<String> fontesUpstream,
        List<String> comentariosValidacaoHumana
) {

    public ItemRuntimeInterpretado {
        fontesUpstream = fontesUpstream == null ? List.of() : List.copyOf(fontesUpstream);
        comentariosValidacaoHumana = comentariosValidacaoHumana == null
                ? List.of()
                : List.copyOf(comentariosValidacaoHumana);
    }

    @Override
    public List<String> fontesUpstream() {
        return List.copyOf(fontesUpstream);
    }

    @Override
    public List<String> comentariosValidacaoHumana() {
        return List.copyOf(comentariosValidacaoHumana);
    }
}

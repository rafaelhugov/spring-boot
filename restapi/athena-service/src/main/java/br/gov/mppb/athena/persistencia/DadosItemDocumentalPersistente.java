package br.gov.mppb.athena.persistencia;

import java.util.List;
import java.util.UUID;

public record DadosItemDocumentalPersistente(
        UUID id,
        UUID idVersao,
        String identificador,
        String titulo,
        String conteudo,
        int ordem,
        List<String> fontesRastreabilidade
) {

    public DadosItemDocumentalPersistente {
        fontesRastreabilidade = fontesRastreabilidade == null ? List.of() : List.copyOf(fontesRastreabilidade);
    }

    @Override
    public List<String> fontesRastreabilidade() {
        return List.copyOf(fontesRastreabilidade);
    }
}

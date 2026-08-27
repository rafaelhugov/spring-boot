package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;

import java.util.Optional;

public record ReferenciaAvaliacaoDocumental(
        IdentificadorProjeto projeto,
        String documento,
        int versao,
        String item
) {

    public ReferenciaAvaliacaoDocumental {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto da avaliacao deve ser informado.");
        }
        documento = TextoAplicacao.validar(documento, "documento da avaliacao");
        if (versao < 1) {
            throw new IllegalArgumentException("versao da avaliacao deve ser positiva.");
        }
        item = item == null || item.isBlank() ? null : item.strip();
    }

    public Optional<String> itemAvaliado() {
        return Optional.ofNullable(item);
    }
}

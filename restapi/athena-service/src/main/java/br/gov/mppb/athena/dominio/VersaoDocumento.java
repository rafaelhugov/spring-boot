package br.gov.mppb.athena.dominio;

import java.time.OffsetDateTime;
import java.util.List;

public record VersaoDocumento(
        int numero,
        FormatoDocumento formato,
        String conteudoOriginal,
        OffsetDateTime dataRecebimento,
        List<ItemDocumental> itens
) {

    public VersaoDocumento {
        if (numero < 1) {
            throw new IllegalArgumentException("numero da versao deve ser positivo.");
        }
        if (formato == null) {
            throw new IllegalArgumentException("formato da versao deve ser informado.");
        }
        conteudoOriginal = TextoObrigatorio.validar(conteudoOriginal, "conteudo original da versao");
        if (dataRecebimento == null) {
            throw new IllegalArgumentException("data de recebimento da versao deve ser informada.");
        }
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}

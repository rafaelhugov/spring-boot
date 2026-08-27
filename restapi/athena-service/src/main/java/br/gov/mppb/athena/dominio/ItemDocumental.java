package br.gov.mppb.athena.dominio;

import java.util.Set;

public record ItemDocumental(
        String identificador,
        String titulo,
        String conteudo,
        Set<RelacaoDocumental> relacoesRecebidas,
        RastreabilidadeDocumento rastreabilidade
) {

    public ItemDocumental {
        identificador = TextoObrigatorio.validar(identificador, "identificador do item documental");
        titulo = TextoObrigatorio.validar(titulo, "titulo do item documental");
        conteudo = TextoObrigatorio.validar(conteudo, "conteudo do item documental");
        relacoesRecebidas = relacoesRecebidas == null ? Set.of() : Set.copyOf(relacoesRecebidas);
        if (rastreabilidade == null) {
            throw new IllegalArgumentException("rastreabilidade do item documental deve ser informada.");
        }
    }

    public boolean possuiRelacoesExplicitamenteRecebidas() {
        return !relacoesRecebidas.isEmpty();
    }
}

package br.gov.mppb.athena.dominio;

import java.util.Set;

public record MetadadosDocumento(
        String grupo,
        String categoria,
        String finalidade,
        String apresentacao,
        Set<PermissaoDocumento> permissoes,
        Set<RelacaoDocumental> relacoesRecebidas,
        RastreabilidadeDocumento rastreabilidade
) {

    public MetadadosDocumento {
        grupo = TextoObrigatorio.validar(grupo, "grupo do documento");
        categoria = TextoObrigatorio.validar(categoria, "categoria do documento");
        finalidade = TextoObrigatorio.validar(finalidade, "finalidade do documento");
        apresentacao = TextoObrigatorio.validar(apresentacao, "apresentacao do documento");
        if (permissoes == null || permissoes.isEmpty()) {
            throw new IllegalArgumentException("permissoes do documento devem ser informadas.");
        }
        permissoes = Set.copyOf(permissoes);
        relacoesRecebidas = relacoesRecebidas == null ? Set.of() : Set.copyOf(relacoesRecebidas);
        if (rastreabilidade == null) {
            throw new IllegalArgumentException("rastreabilidade do documento deve ser informada.");
        }
    }

    public boolean possuiRelacoesExplicitamenteRecebidas() {
        return !relacoesRecebidas.isEmpty();
    }
}

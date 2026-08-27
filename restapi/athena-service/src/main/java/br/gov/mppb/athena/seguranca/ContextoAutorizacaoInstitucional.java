package br.gov.mppb.athena.seguranca;

import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import java.util.Set;

public record ContextoAutorizacaoInstitucional(
        UsuarioInstitucional usuario,
        Set<String> autorizacoes
) {

    public ContextoAutorizacaoInstitucional {
        autorizacoes = autorizacoes == null ? Set.of() : Set.copyOf(autorizacoes);
    }

    public boolean autenticado() {
        return usuario != null;
    }

    public boolean possuiAutorizacao(String autorizacao) {
        return autorizacoes.contains(autorizacao);
    }
}

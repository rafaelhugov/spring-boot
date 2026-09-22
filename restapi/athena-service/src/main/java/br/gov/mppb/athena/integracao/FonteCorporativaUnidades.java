package br.gov.mppb.athena.integracao;

import java.util.List;

@FunctionalInterface
public interface FonteCorporativaUnidades {
    List<UnidadeCorporativa> pesquisar(String termo);
}

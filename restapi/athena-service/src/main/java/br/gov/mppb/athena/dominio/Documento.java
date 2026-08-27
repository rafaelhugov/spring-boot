package br.gov.mppb.athena.dominio;

import java.util.List;

public record Documento(
        String identificador,
        IdentificadorProjeto projeto,
        String titulo,
        MetadadosDocumento metadados,
        List<VersaoDocumento> versoes
) {

    public Documento {
        identificador = TextoObrigatorio.validar(identificador, "identificador do documento");
        if (projeto == null) {
            throw new IllegalArgumentException("projeto do documento deve ser informado.");
        }
        titulo = TextoObrigatorio.validar(titulo, "titulo do documento");
        if (metadados == null) {
            throw new IllegalArgumentException("metadados do documento devem ser informados.");
        }
        versoes = versoes == null ? List.of() : List.copyOf(versoes);
    }
}

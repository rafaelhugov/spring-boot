package br.gov.mppb.athena.dominio;

public record IdentificadorUsuario(String valor) {

    public IdentificadorUsuario {
        valor = TextoObrigatorio.validar(valor, "identificador do usuario");
    }
}

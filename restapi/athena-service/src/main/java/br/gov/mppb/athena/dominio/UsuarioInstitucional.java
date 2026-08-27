package br.gov.mppb.athena.dominio;

public record UsuarioInstitucional(
        IdentificadorUsuario identificador,
        String nome,
        String login,
        String unidade
) {

    public UsuarioInstitucional {
        if (identificador == null) {
            throw new IllegalArgumentException("identificador do usuario deve ser informado.");
        }
        nome = TextoObrigatorio.validar(nome, "nome do usuario");
        login = TextoObrigatorio.validar(login, "login do usuario");
        unidade = TextoObrigatorio.validar(unidade, "unidade do usuario");
    }
}

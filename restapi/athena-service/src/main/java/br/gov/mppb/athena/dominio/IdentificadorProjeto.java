package br.gov.mppb.athena.dominio;

public record IdentificadorProjeto(String valor) {

    public IdentificadorProjeto {
        valor = TextoObrigatorio.validar(valor, "identificador do projeto");
        PoliticaDominioMultiprojeto.rejeitarTermoDeProjetoPiloto(valor, "identificador do projeto");
    }
}

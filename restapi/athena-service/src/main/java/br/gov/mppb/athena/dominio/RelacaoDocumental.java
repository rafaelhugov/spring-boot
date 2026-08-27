package br.gov.mppb.athena.dominio;

public record RelacaoDocumental(
        String tipo,
        String recursoRelacionado
) {

    public RelacaoDocumental {
        tipo = TextoObrigatorio.validar(tipo, "tipo da relacao documental");
        recursoRelacionado = TextoObrigatorio.validar(recursoRelacionado, "recurso relacionado");
    }
}

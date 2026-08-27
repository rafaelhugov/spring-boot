package br.gov.mppb.athena.api;

public record ErroFuncionalApi(
        int status,
        String codigo,
        String descricao
) {

    public ErroFuncionalApi {
        if (status < 400) {
            throw new IllegalArgumentException("status de erro funcional deve ser igual ou superior a 400.");
        }
        codigo = TextoApi.validar(codigo, "codigo do erro funcional");
        descricao = TextoApi.validar(descricao, "descricao do erro funcional");
    }
}

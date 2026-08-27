package br.gov.mppb.athena.seguranca;

public record ResultadoAutorizacao(
        boolean permitido,
        String motivo
) {

    public static ResultadoAutorizacao permitir(String motivo) {
        return new ResultadoAutorizacao(true, motivo);
    }

    public static ResultadoAutorizacao negar(String motivo) {
        return new ResultadoAutorizacao(false, motivo);
    }
}

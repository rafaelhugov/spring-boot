package br.gov.mppb.athena.integracao;

public record ContratoIntegracaoDocumentalExterna(
        String identificador,
        boolean dependenciaExternaDisponivel,
        String tratamentoIndisponibilidade
) {
}

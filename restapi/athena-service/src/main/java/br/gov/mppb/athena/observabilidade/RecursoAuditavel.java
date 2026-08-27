package br.gov.mppb.athena.observabilidade;

public record RecursoAuditavel(
        String tipo,
        String identificador
) {

    public RecursoAuditavel {
        tipo = textoObrigatorio(tipo, "tipo do recurso auditavel");
        identificador = textoObrigatorio(identificador, "identificador do recurso auditavel");
    }

    private static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.strip();
    }
}

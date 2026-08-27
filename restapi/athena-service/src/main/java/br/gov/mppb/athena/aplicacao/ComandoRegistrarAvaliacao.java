package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ComandoRegistrarAvaliacao(
        ReferenciaAvaliacaoDocumental referencia,
        IdentificadorUsuario responsavel,
        OffsetDateTime data,
        DecisaoAvaliacao decisao,
        String justificativa,
        boolean avaliacaoHabilitadaPorPolitica
) {

    public ComandoRegistrarAvaliacao {
        if (referencia == null) {
            throw new IllegalArgumentException("referencia da avaliacao deve ser informada.");
        }
        if (responsavel == null) {
            throw new IllegalArgumentException("responsavel da avaliacao deve ser informado.");
        }
        if (data == null) {
            throw new IllegalArgumentException("data da avaliacao deve ser informada.");
        }
        if (decisao == null) {
            throw new IllegalArgumentException("decisao da avaliacao deve ser informada.");
        }
        justificativa = justificativa == null ? null : TextoAplicacao.validar(justificativa, "justificativa da avaliacao");
    }
}

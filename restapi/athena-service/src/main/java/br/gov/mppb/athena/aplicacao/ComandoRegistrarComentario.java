package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ComandoRegistrarComentario(
        ReferenciaAvaliacaoDocumental referencia,
        IdentificadorUsuario autor,
        OffsetDateTime data,
        String texto,
        DecisaoAvaliacao decisaoRelacionada
) {

    public ComandoRegistrarComentario {
        if (referencia == null) {
            throw new IllegalArgumentException("referencia do comentario deve ser informada.");
        }
        if (autor == null) {
            throw new IllegalArgumentException("autor do comentario deve ser informado.");
        }
        if (data == null) {
            throw new IllegalArgumentException("data do comentario deve ser informada.");
        }
        texto = TextoAplicacao.validar(texto, "texto do comentario");
    }
}

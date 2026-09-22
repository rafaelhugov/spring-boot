package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DadosProjetoPersistente(
        UUID id,
        String identificador,
        String nome,
        String descricao,
        String situacao,
        Integer idUnidadeDemandante,
        String nomeUnidadeDemandante,
        Integer idUnidadeDesenvolvedora,
        String nomeUnidadeDesenvolvedora,
        OffsetDateTime dataCriacao,
        UUID idUsuarioCriacao
) {
    public DadosProjetoPersistente(
            UUID id, String identificador, String nome, String descricao, String situacao,
            OffsetDateTime dataCriacao, UUID idUsuarioCriacao
    ) {
        this(id, identificador, nome, descricao, situacao, null, null, null, null, dataCriacao, idUsuarioCriacao);
    }
}

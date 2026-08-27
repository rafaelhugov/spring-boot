package br.gov.mppb.athena.persistencia;

import java.util.UUID;

public record DadosUsuarioPersistente(
        UUID id,
        String identificador,
        String nome,
        String login,
        String unidade
) {
}

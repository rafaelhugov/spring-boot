package mppb.restapi.cursos.feature.participante.enums;

import lombok.Getter;

/**
 * Enum para representar o tipo de um participante (Interno ou Externo).
 * Garante a consistência dos dados e facilita a implementação de regras de negócio.
 */
@Getter
public enum TipoParticipanteEnum {
    INTERNO("I", "Interno"),
    EXTERNO("E", "Externo");

    private final String sigla;
    private final String descricao;

    TipoParticipanteEnum(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }
}

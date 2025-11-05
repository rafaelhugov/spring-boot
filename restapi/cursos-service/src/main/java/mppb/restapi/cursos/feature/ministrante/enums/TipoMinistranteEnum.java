package mppb.restapi.cursos.feature.ministrante.enums;

import lombok.Getter;

@Getter
public enum TipoMinistranteEnum {
    INTERNO('I'),
    EXTERNO('E');

    private final char codigo;

    TipoMinistranteEnum(char codigo) {
        this.codigo = codigo;
    }

    public static TipoMinistranteEnum fromCodigo(Character codigo) {
        if (codigo == null) {
            return null;
        }
        for (TipoMinistranteEnum tipo : TipoMinistranteEnum.values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de TipoMinistranteEnum inválido: " + codigo);
    }
}

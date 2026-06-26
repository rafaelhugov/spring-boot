package mppb.restapi.cursos.feature.curso.enums;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum StatusCursoEnum {
    EM_CONSTRUCAO('C', "EM CONSTRUÇÃO"),
    PUBLICADO('P', "PUBLICADO"),
    FINALIZADO('F', "FINALIZADO");

    private final Character codigo;
    private final String descricao;

    StatusCursoEnum(Character codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public static StatusCursoEnum fromCodigo(Character codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("Código de status de curso não pode ser nulo.");
        }
        return Stream.of(StatusCursoEnum.values())
                .filter(s -> s.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Código de status inválido: " + codigo));
    }
}

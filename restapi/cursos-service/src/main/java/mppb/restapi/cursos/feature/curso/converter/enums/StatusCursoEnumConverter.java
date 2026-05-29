package mppb.restapi.cursos.feature.curso.converter.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class StatusCursoEnumConverter implements AttributeConverter<StatusCursoEnum, Character> {

    @Override
    public Character convertToDatabaseColumn(StatusCursoEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCodigo();
    }

    @Override
    public StatusCursoEnum convertToEntityAttribute(Character dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(StatusCursoEnum.values())
                .filter(s -> s.getCodigo().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Código de status de curso inválido: " + dbData));
    }
}

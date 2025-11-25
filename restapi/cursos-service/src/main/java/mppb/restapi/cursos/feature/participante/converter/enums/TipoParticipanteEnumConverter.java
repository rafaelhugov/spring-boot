package mppb.restapi.cursos.feature.participante.converter.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class TipoParticipanteEnumConverter implements AttributeConverter<TipoParticipanteEnum, String> {

    @Override
    public String convertToDatabaseColumn(TipoParticipanteEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getSigla();
    }

    @Override
    public TipoParticipanteEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(TipoParticipanteEnum.values())
                .filter(c -> c.getSigla().equals(dbData))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}

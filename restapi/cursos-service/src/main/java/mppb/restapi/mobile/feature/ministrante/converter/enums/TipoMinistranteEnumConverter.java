package mppb.restapi.mobile.feature.ministrante.converter.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import mppb.restapi.mobile.feature.ministrante.enums.TipoMinistranteEnum;

@Converter(autoApply = true)
public class TipoMinistranteEnumConverter implements AttributeConverter<TipoMinistranteEnum, Character> {

    @Override
    public Character convertToDatabaseColumn(TipoMinistranteEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCodigo();
    }

    @Override
    public TipoMinistranteEnum convertToEntityAttribute(Character dbData) {
        if (dbData == null) {
            return null;
        }
        return TipoMinistranteEnum.fromCodigo(dbData);
    }
}

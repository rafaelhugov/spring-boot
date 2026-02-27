package mppb.restapi.mobile.feature.inscricao.converter.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import mppb.restapi.mobile.feature.inscricao.enums.SituacaoInscricaoEnum;

import java.util.stream.Stream;

/**
 * Converte a Enum SituacaoInscricaoEnum para seu correspondente caractere no banco de dados e vice-versa.
 * A anotação @Converter(autoApply = true) garante que o JPA use este conversor automaticamente
 * para todos os atributos do tipo SituacaoInscricaoEnum.
 */
@Converter(autoApply = true)
public class SituacaoInscricaoEnumConverter implements AttributeConverter<SituacaoInscricaoEnum, Character> {

    @Override
    public Character convertToDatabaseColumn(SituacaoInscricaoEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCodigo();
    }

    @Override
    public SituacaoInscricaoEnum convertToEntityAttribute(Character dbData) {
        if (dbData == null) {
            return null;
        }
        // A lógica de conversão agora reside aqui, fora da Enum.
        return Stream.of(SituacaoInscricaoEnum.values())
                .filter(s -> s.getCodigo().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Código de situação inválido: " + dbData));
    }
}

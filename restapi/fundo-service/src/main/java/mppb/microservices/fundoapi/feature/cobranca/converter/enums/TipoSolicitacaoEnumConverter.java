package mppb.microservices.fundoapi.feature.cobranca.converter.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import mppb.microservices.fundoapi.feature.cobranca.enums.TipoSolicitacaoEnum;

@Converter(autoApply = false)
public class TipoSolicitacaoEnumConverter implements AttributeConverter<TipoSolicitacaoEnum, String> {

    @Override
    public String convertToDatabaseColumn(TipoSolicitacaoEnum escopo) {
        return escopo != null ? escopo.getCodigo() : null;
    }

    @Override
    public TipoSolicitacaoEnum convertToEntityAttribute(String dbData) {
        return dbData != null ? TipoSolicitacaoEnum.fromCodigo(dbData) : null;
    }
}
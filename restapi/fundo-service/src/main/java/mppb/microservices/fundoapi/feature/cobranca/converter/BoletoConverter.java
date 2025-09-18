package mppb.microservices.fundoapi.feature.cobranca.converter;

import mppb.microservices.common.converter.AbstractConverter;
import mppb.microservices.fundoapi.feature.cobranca.dto.BoletoDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Boleto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class BoletoConverter extends AbstractConverter<BoletoDto, Boleto> {

    public BoletoConverter(ModelMapper modelMapper) {
        super(modelMapper, BoletoDto.class, Boleto.class);
    }
}
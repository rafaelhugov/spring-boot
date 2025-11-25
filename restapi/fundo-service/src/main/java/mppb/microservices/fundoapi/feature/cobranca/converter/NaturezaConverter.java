package mppb.microservices.fundoapi.feature.cobranca.converter;

import mppb.microservices.common.core.converter.AbstractConverter;
import mppb.microservices.fundoapi.feature.cobranca.dto.NaturezaDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Natureza;
import mppb.microservices.fundoapi.feature.cobranca.enums.TipoSolicitacaoEnum;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class NaturezaConverter extends AbstractConverter<NaturezaDto, Natureza> {

    public NaturezaConverter(ModelMapper modelMapper) {
        super(modelMapper, NaturezaDto.class, Natureza.class);

        mapEnumField(
                modelMapper,
                Natureza.class,
                NaturezaDto.class,
                Natureza::getTipo,
                NaturezaDto::setTipo,
                NaturezaDto::setTipoDescricao,
                TipoSolicitacaoEnum::getLabel
        );

    }
}
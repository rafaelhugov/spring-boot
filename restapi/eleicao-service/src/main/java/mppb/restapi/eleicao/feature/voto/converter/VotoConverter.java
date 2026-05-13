package mppb.restapi.eleicao.feature.voto.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.eleicao.feature.voto.dto.VotoDto;
import mppb.restapi.eleicao.feature.voto.entity.Voto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class VotoConverter extends EntityConverterImpl<VotoDto, Voto> {
    public VotoConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

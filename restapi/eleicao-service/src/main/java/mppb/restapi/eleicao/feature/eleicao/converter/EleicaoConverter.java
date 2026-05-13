package mppb.restapi.eleicao.feature.eleicao.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.eleicao.feature.eleicao.dto.EleicaoDto;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class EleicaoConverter extends EntityConverterImpl<EleicaoDto, Eleicao> {
    public EleicaoConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

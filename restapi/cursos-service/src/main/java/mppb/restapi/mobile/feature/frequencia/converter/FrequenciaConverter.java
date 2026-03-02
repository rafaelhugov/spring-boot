package mppb.restapi.mobile.feature.frequencia.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.frequencia.dto.FrequenciaDto;
import mppb.restapi.mobile.feature.frequencia.entity.Frequencia;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class FrequenciaConverter extends EntityConverterImpl<FrequenciaDto, Frequencia> {
    public FrequenciaConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }

}

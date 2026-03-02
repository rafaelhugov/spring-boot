package mppb.restapi.cursos.feature.frequencia.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.frequencia.dto.FrequenciaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Frequencia;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class FrequenciaConverter extends EntityConverterImpl<FrequenciaDto, Frequencia> {
    public FrequenciaConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }

}

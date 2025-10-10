package mppb.restapi.cursos.feature.ministrante.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteDto;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class MinistranteConverter extends EntityConverterImpl<MinistranteDto, Ministrante> {
    public MinistranteConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

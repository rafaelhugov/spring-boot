package mppb.restapi.mobile.feature.ministrante.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.ministrante.dto.MinistranteDto;
import mppb.restapi.mobile.feature.ministrante.entity.Ministrante;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class MinistranteConverter extends EntityConverterImpl<MinistranteDto, Ministrante> {
    public MinistranteConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

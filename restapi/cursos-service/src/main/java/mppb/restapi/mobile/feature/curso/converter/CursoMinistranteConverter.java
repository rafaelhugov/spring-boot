package mppb.restapi.mobile.feature.curso.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.curso.dto.CursoMinistranteDto;
import mppb.restapi.mobile.feature.curso.entity.CursoMinistrante;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CursoMinistranteConverter extends EntityConverterImpl<CursoMinistranteDto, CursoMinistrante> {
    public CursoMinistranteConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

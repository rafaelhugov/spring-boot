package mppb.restapi.cursos.feature.lookup.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.lookup.dto.MunicipioDto;
import mppb.restapi.cursos.feature.lookup.entities.Municipio;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class MunicipioConverter extends EntityConverterImpl<MunicipioDto, Municipio> {
    public MunicipioConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

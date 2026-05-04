package mppb.restapi.cursos.feature.lookup.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.lookup.dto.LocalEventoDto;
import mppb.restapi.cursos.feature.lookup.entities.LocalEvento;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class LocalEventoConverter extends EntityConverterImpl<LocalEventoDto, LocalEvento> {
    public LocalEventoConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

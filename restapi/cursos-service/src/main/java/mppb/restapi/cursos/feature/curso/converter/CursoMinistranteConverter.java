package mppb.restapi.cursos.feature.curso.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.curso.dto.CursoMinistranteDto;
import mppb.restapi.cursos.feature.curso.entity.CursoMinistrante;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CursoMinistranteConverter extends EntityConverterImpl<CursoMinistranteDto, CursoMinistrante> {
    public CursoMinistranteConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

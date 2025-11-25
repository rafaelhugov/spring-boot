package mppb.restapi.cursos.feature.curso.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


@Component
public class CursoConverter extends EntityConverterImpl<CursoDto, Curso>  {
    public CursoConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

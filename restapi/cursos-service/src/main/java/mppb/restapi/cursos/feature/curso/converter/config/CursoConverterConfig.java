package mppb.restapi.cursos.feature.curso.converter.config;

import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CursoConverterConfig {

//    @Bean
//    public DtoConverter<CursoDto, Curso> cursoConverter(ModelMapper modelMapper) {
//        return new EntityConverterImpl<CursoDto, Curso>(modelMapper) {};
//    }
}

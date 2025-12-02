package mppb.restapi.cursos.feature.frequencia.converter;

import lombok.RequiredArgsConstructor;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.frequencia.dto.AulaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AulaConverter extends EntityConverterImpl<AulaDto, Aula> {
    public AulaConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    public void map(Aula from, Aula to) {
        to.setHoraFim(from.getHoraFim());
        to.setHoraInicio(from.getHoraInicio());
        to.setDescricao(from.getDescricao());
        to.setData(from.getData());
    }
}


package mppb.restapi.cursos.feature.frequencia.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.frequencia.dto.ChamadaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ChamadaConverter extends EntityConverterImpl<ChamadaDto, Chamada> {
    public ChamadaConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

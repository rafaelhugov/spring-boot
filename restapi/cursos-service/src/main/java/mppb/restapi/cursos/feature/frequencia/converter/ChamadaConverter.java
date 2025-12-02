package mppb.restapi.cursos.feature.frequencia.converter;

import lombok.RequiredArgsConstructor;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.frequencia.dto.ChamadaDto;
import mppb.restapi.cursos.feature.frequencia.dto.FrequenciaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.entity.Frequencia;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ChamadaConverter extends EntityConverterImpl<ChamadaDto, Chamada> {
    public ChamadaConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

package mppb.restapi.mobile.feature.frequencia.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.frequencia.dto.ChamadaDto;
import mppb.restapi.mobile.feature.frequencia.entity.Chamada;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ChamadaConverter extends EntityConverterImpl<ChamadaDto, Chamada> {
    public ChamadaConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

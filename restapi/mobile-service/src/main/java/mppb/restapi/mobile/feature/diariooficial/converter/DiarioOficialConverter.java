package mppb.restapi.cursos.feature.diariooficial.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.diariooficial.dto.DiarioOficialDto;
import mppb.restapi.cursos.feature.diariooficial.entity.DiarioOficial;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


@Component
public class DiarioOficialConverter extends EntityConverterImpl<DiarioOficialDto, DiarioOficial>  {
    public DiarioOficialConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

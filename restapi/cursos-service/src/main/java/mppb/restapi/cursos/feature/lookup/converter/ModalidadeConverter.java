package mppb.restapi.cursos.feature.lookup.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.lookup.dto.ModalidadeDto;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ModalidadeConverter extends EntityConverterImpl<ModalidadeDto, Modalidade> {

    public ModalidadeConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }

}

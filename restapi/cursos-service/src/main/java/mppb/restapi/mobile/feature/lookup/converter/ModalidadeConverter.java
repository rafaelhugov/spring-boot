package mppb.restapi.mobile.feature.lookup.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.lookup.dto.ModalidadeDto;
import mppb.restapi.mobile.feature.lookup.entities.Modalidade;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ModalidadeConverter extends EntityConverterImpl<ModalidadeDto, Modalidade> {

    public ModalidadeConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }

}

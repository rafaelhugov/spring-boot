package mppb.microservices.cursosapi.feature.lookup.converter;

import mppb.microservices.common.core.converter.EntityConverterImpl;
import mppb.microservices.cursosapi.feature.lookup.dto.ModalidadeDto;
import mppb.microservices.cursosapi.feature.lookup.entities.Modalidade;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ModalidadeConverter extends EntityConverterImpl<ModalidadeDto, Modalidade> {

    public ModalidadeConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }

}

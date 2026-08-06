package br.gov.mppb.ncap.feature.entidade.converter;

import br.gov.mppb.ncap.feature.entidade.Entidade;
import br.gov.mppb.ncap.feature.entidade.dto.EntidadeDto;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class EntidadeConverter extends EntityConverterImpl<EntidadeDto, Entidade> {

    public EntidadeConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

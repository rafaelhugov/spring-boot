package mppb.restapi.eleicao.feature.candidato.converter;

import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.eleicao.feature.candidato.dto.CandidatoDto;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CandidatoConverter extends EntityConverterImpl<CandidatoDto, Candidato> {
    public CandidatoConverter(ModelMapper modelMapper) {
        super(modelMapper);
    }
}

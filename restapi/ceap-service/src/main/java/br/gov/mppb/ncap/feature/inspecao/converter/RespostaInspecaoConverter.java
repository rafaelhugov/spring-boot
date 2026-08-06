package br.gov.mppb.ncap.feature.inspecao.converter;

import br.gov.mppb.ncap.feature.formtemplate.Pergunta;
import br.gov.mppb.ncap.feature.inspecao.Inspecao;
import br.gov.mppb.ncap.feature.inspecao.RespostaInspecao;
import br.gov.mppb.ncap.feature.inspecao.dto.RespostaInspecaoDto;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RespostaInspecaoConverter extends EntityConverterImpl<RespostaInspecaoDto, RespostaInspecao> {

    public RespostaInspecaoConverter(ModelMapper modelMapper) {
        super(modelMapper);

        modelMapper.typeMap(RespostaInspecao.class, RespostaInspecaoDto.class).addMappings(mapper -> {
            mapper.map(src -> src.getInspecao().getId(), RespostaInspecaoDto::setIdInspecao);
            mapper.map(src -> src.getPergunta().getId(), RespostaInspecaoDto::setIdPergunta);
        });

        modelMapper.typeMap(RespostaInspecaoDto.class, RespostaInspecao.class).addMappings(mapper -> {
            mapper.skip(RespostaInspecao::setInspecao);
            mapper.skip(RespostaInspecao::setPergunta);
        });
    }

    @Override
    public RespostaInspecao toEntity(RespostaInspecaoDto dto) {
        RespostaInspecao entity = super.toEntity(dto);
        if (dto.getIdInspecao() != null) {
            Inspecao inspecao = new Inspecao();
            inspecao.setId(dto.getIdInspecao());
            entity.setInspecao(inspecao);
        }
        if (dto.getIdPergunta() != null) {
            Pergunta pergunta = new Pergunta();
            pergunta.setId(dto.getIdPergunta());
            entity.setPergunta(pergunta);
        }
        return entity;
    }
}

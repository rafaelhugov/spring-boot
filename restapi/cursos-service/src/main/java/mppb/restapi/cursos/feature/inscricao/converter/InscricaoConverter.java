package mppb.restapi.cursos.feature.inscricao.converter;

import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.participante.converter.ParticipanteConverter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class InscricaoConverter implements DtoConverter<InscricaoDto, Inscricao> {

    private final ModelMapper modelMapper;
    private final ParticipanteConverter participanteConverter;

    public InscricaoConverter(ModelMapper modelMapper, ParticipanteConverter participanteConverter) {
        this.modelMapper = modelMapper;
        this.participanteConverter = participanteConverter;
    }

    @Override
    public InscricaoDto toDto(Inscricao entity) {
        if (entity == null) {
            return null;
        }

        InscricaoDto dto = modelMapper.map(entity, InscricaoDto.class);

        // Garante que a descrição da Enum seja usada, e não seu nome.
        if (entity.getSituacao() != null) {
            dto.setSituacao(entity.getSituacao().getDescricao());
        }

        // Usa o converter dedicado para o objeto aninhado, garantindo a separação de responsabilidades.
        if (entity.getParticipante() != null) {
            dto.setParticipante(participanteConverter.toDto(entity.getParticipante()));
        }

        return dto;
    }

    @Override
    public Inscricao toEntity(InscricaoDto dto) {
        // A conversão de DTO para entidade para este recurso não é necessária
        // pois a criação/atualização usam DTOs específicos.
        if (dto == null) {
            return null;
        }
        return modelMapper.map(dto, Inscricao.class);
    }
}


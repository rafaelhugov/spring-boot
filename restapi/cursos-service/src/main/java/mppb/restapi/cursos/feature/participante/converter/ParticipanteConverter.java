package mppb.restapi.cursos.feature.participante.converter;

import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ParticipanteConverter {

    private final ModelMapper modelMapper;

    public ParticipanteConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public ParticipanteDto toDto(Participante entity) {
        ParticipanteDto dto = modelMapper.map(entity, ParticipanteDto.class);

        // RN012 - Lógica para enriquecer dados de participantes internos (placeholder)
        // No futuro, se o participante for INTERNO, aqui seria o local para
        // chamar um serviço do RH para buscar o nome, CPF e e-mail e preenchê-los no DTO.
        // ex: if (dto.getTipo() == TipoParticipanteEnum.INTERNO) { ... }

        return dto;
    }

    public List<ParticipanteDto> toDto(List<Participante> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}

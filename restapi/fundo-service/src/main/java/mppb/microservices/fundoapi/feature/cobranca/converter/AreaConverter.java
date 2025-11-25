package mppb.microservices.fundoapi.feature.cobranca.converter;

import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import mppb.microservices.fundoapi.feature.cobranca.entity.Area;
import mppb.microservices.common.core.converter.AbstractConverter;

@Component
public class AreaConverter extends AbstractConverter<AreaDto, Area> {

    public AreaConverter(ModelMapper modelMapper) {
        super(modelMapper, AreaDto.class, Area.class);
    }

    // Opcional: Pode sobrescrever os métodos para adicionar lógica específica
//    @Override
//    public AreaDto toDto(Area entity) {
//        return super.toDto(entity);
//    }
//
//    @Override
//    public Area toEntity(AreaDto dto) {
//        return super.toEntity(dto);
//    }
}
package mppb.restapi.mobile.feature.cadastro.converter.config;

import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.restapi.mobile.feature.cadastro.entities.EntidadeTutora;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CadastroConverterConfig {

    @Bean
    public DtoConverter<EntidadeTutoraDto, EntidadeTutora> entidadeTutoraConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<EntidadeTutoraDto, EntidadeTutora>(modelMapper) {};
    }
}

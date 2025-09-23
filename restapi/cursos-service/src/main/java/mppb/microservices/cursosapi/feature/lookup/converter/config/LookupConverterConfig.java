package mppb.microservices.cursosapi.feature.lookup.converter.config;

import mppb.microservices.common.core.converter.EntityConverter;
import mppb.microservices.common.core.converter.EntityConverterImpl;
import mppb.microservices.cursosapi.feature.lookup.dto.PublicoAlvoDto;
import mppb.microservices.cursosapi.feature.lookup.dto.TipoEventoDto;
import mppb.microservices.cursosapi.feature.lookup.entities.PublicoAlvo;
import mppb.microservices.cursosapi.feature.lookup.entities.TipoEvento;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LookupConverterConfig {

    @Bean
    public EntityConverter<TipoEventoDto, TipoEvento> tipoEventoConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<TipoEventoDto, TipoEvento>(modelMapper) {};
    }

    @Bean
    public EntityConverter<PublicoAlvoDto, PublicoAlvo> publicoAlvoConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<PublicoAlvoDto, PublicoAlvo>(modelMapper) {};
    }
}

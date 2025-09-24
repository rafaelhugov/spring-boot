package mppb.microservices.cursosapi.feature.cadastro.converter.config;

import mppb.microservices.common.core.converter.EntityConverter;
import mppb.microservices.common.core.converter.EntityConverterImpl;
import mppb.microservices.cursosapi.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.microservices.cursosapi.feature.cadastro.entities.EntidadeTutora;
import mppb.microservices.cursosapi.feature.lookup.dto.PapelMinistranteDto;
import mppb.microservices.cursosapi.feature.lookup.dto.PublicoAlvoDto;
import mppb.microservices.cursosapi.feature.lookup.dto.TipoEventoDto;
import mppb.microservices.cursosapi.feature.lookup.entities.PapelMinistrante;
import mppb.microservices.cursosapi.feature.lookup.entities.PublicoAlvo;
import mppb.microservices.cursosapi.feature.lookup.entities.TipoEvento;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CadastroConverterConfig {

    @Bean
    public EntityConverter<EntidadeTutoraDto, EntidadeTutora> entidadeTutoraConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<EntidadeTutoraDto, EntidadeTutora>(modelMapper) {};
    }
}

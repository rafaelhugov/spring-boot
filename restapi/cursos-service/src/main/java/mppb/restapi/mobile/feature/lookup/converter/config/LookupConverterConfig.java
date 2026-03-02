package mppb.restapi.mobile.feature.lookup.converter.config;

import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.mobile.feature.lookup.dto.PapelMinistranteDto;
import mppb.restapi.mobile.feature.lookup.dto.PublicoAlvoDto;
import mppb.restapi.mobile.feature.lookup.dto.TipoEventoDto;
import mppb.restapi.mobile.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.mobile.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.mobile.feature.lookup.entities.TipoEvento;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LookupConverterConfig {

    @Bean
    public DtoConverter<TipoEventoDto, TipoEvento> tipoEventoConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<TipoEventoDto, TipoEvento>(modelMapper) {};
    }

    @Bean
    public DtoConverter<PublicoAlvoDto, PublicoAlvo> publicoAlvoConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<PublicoAlvoDto, PublicoAlvo>(modelMapper) {};
    }

    @Bean
    public DtoConverter<PapelMinistranteDto, PapelMinistrante> papelMinistranteConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<PapelMinistranteDto, PapelMinistrante>(modelMapper) {};
    }
}

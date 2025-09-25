package mppb.restapi.cursos.feature.lookup.converter.config;

import mppb.restapi.common.core.converter.EntityConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.lookup.dto.PapelMinistranteDto;
import mppb.restapi.cursos.feature.lookup.dto.PublicoAlvoDto;
import mppb.restapi.cursos.feature.lookup.dto.TipoEventoDto;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
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

    @Bean
    public EntityConverter<PapelMinistranteDto, PapelMinistrante> papelMinistranteConverter(ModelMapper modelMapper) {
        return new EntityConverterImpl<PapelMinistranteDto, PapelMinistrante>(modelMapper) {};
    }
}

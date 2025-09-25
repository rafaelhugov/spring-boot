package mppb.restapi.cursos.feature.cadastro.converter.config;

import mppb.restapi.common.core.converter.EntityConverter;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import mppb.restapi.cursos.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.lookup.dto.PapelMinistranteDto;
import mppb.restapi.cursos.feature.lookup.dto.PublicoAlvoDto;
import mppb.restapi.cursos.feature.lookup.dto.TipoEventoDto;
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

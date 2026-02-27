package mppb.restapi.mobile.infrastructure.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        //modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        //modelMapper.registerModule(new RecordModule()); // Enable Record Support
        return modelMapper;
    }
}
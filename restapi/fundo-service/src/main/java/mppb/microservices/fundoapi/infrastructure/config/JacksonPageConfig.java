package mppb.microservices.fundoapi.infrastructure.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.Module;
import mppb.microservices.common.core.jackson.PageJsonSerializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

@Configuration
public class JacksonPageConfig {

//    @Bean(name = "customPageJacksonModule")
    public Module pageJacksonModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer((Class<Page<?>>)(Class<?>) Page.class, new PageJsonSerializer());
        return module;
    }

//    @Bean
//    public Jackson2ObjectMapperBuilderCustomizer pageModuleCustomizer() {
//        return builder -> builder.modules(pageJacksonModule());
//    }

}
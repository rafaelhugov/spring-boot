package mppb.restapi.mobile.infrastructure.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.Module;
import mppb.restapi.common.core.jackson.PageJsonSerializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

@Configuration
public class JacksonPageConfig {

    public Module pageJacksonModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer((Class<Page<?>>)(Class<?>) Page.class, new PageJsonSerializer());
        return module;
    }

}
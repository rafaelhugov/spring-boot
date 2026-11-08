package mppb.restapi.mobile.feature.appversion.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import mppb.restapi.mobile.feature.appversion.interceptor.AppVersionInterceptor;

@Configuration("appVersionWebConfig")
public class AppVersionWebConfig implements WebMvcConfigurer {

    private final AppVersionInterceptor appVersionInterceptor;

    @Autowired
    public AppVersionWebConfig(AppVersionInterceptor appVersionInterceptor) {
        this.appVersionInterceptor = appVersionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appVersionInterceptor).addPathPatterns("/**");
    }
}

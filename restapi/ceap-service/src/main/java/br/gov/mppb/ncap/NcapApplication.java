package br.gov.mppb.ncap;

import mppb.restapi.common.feature.configuracao.config.MailConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(
        basePackages = {"mppb.restapi.common", "br.gov.mppb.ncap"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = MailConfig.class
        )
)
@EntityScan(basePackages = {"mppb.restapi.common", "br.gov.mppb.ncap"})
@EnableJpaRepositories(basePackages = {"mppb.restapi.common", "br.gov.mppb.ncap"})
@EnableFeignClients
@EnableScheduling
public class NcapApplication {

    public static void main(String[] args) {
        SpringApplication.run(NcapApplication.class, args);
    }

}

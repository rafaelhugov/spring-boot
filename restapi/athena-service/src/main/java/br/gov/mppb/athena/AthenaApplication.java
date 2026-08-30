package br.gov.mppb.athena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"br.gov.mppb.athena", "mppb.restapi.common.infrastructure"})
@EnableFeignClients(basePackages = "mppb.restapi.common.infrastructure.client")
public class AthenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AthenaApplication.class, args);
    }
}

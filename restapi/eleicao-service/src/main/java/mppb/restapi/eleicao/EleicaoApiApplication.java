package mppb.restapi.eleicao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"mppb.restapi.common", "mppb.restapi.eleicao"})
@EntityScan(basePackages = {"mppb.restapi.common", "mppb.restapi.eleicao"})
@EnableJpaRepositories(basePackages = {"mppb.restapi.common", "mppb.restapi.eleicao"})
public class EleicaoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EleicaoApiApplication.class, args);
    }

}

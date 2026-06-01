package mppb.restapi.cursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"mppb.restapi.common", "mppb.restapi.cursos"}, exclude = { SecurityAutoConfiguration.class })
@EntityScan(basePackages = {"mppb.restapi.common", "mppb.restapi.cursos"})
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableCaching
@org.springframework.scheduling.annotation.EnableScheduling
@EnableJpaRepositories(basePackages = {"mppb.restapi.common", "mppb.restapi.cursos"})
public class CursosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CursosApiApplication.class, args);
	}

}

package mppb.restapi.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"mppb.restapi.common", "mppb.restapi.cursos", "mppb.restapi.mobile"}, exclude = { SecurityAutoConfiguration.class })
@EntityScan(basePackages = {"mppb.restapi.common", "mppb.restapi.cursos", "mppb.restapi.mobile"})
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackages = {"mppb.restapi.common", "mppb.restapi.cursos", "mppb.restapi.mobile"})
public class MobileApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobileApiApplication.class, args);
	}

}

package mppb.restapi.cursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"mppb.restapi.common", "mppb.restapi.cursos"}, exclude = { SecurityAutoConfiguration.class })
@EnableFeignClients
@EnableCaching
public class CursosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CursosApiApplication.class, args);
	}

}

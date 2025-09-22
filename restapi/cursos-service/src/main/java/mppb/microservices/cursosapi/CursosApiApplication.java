package mppb.microservices.cursosapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"mppb.microservices.common", "mppb.microservices.cursosapi"}, exclude = { SecurityAutoConfiguration.class })
@EnableFeignClients
public class CursosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CursosApiApplication.class, args);
	}

}

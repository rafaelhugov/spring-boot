package mppb.microservices.fundoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"mppb.microservices.common", "mppb.microservices.fundoapi"}, exclude = { SecurityAutoConfiguration.class })
@EnableFeignClients
public class FundoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FundoApiApplication.class, args);
	}

}

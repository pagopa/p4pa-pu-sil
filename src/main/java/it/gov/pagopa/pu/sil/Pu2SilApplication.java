package it.gov.pagopa.pu.sil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
public class Pu2SilApplication {

	public static void main(String[] args) {
		SpringApplication.run(Pu2SilApplication.class, args);
	}

}

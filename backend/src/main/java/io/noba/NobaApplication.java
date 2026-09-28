package io.noba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NobaApplication {

	public static void main(String[] args) {
		SpringApplication.run(NobaApplication.class, args);
	}
}

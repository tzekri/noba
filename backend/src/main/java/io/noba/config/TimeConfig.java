package io.noba.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

	/** Fuseau qui définit « la journée » des tickets (remise à zéro des numéros à minuit). */
	@Bean
	Clock clock(@Value("${app.timezone}") String zone) {
		return Clock.system(ZoneId.of(zone));
	}
}

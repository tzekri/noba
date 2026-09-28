package io.noba.service;

import io.noba.repo.PushSubscriptionRepository;
import io.noba.repo.TicketRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clôture des journées : les tickets restés actifs la veille passent en EXPIRED.
 * Les numéros repartent d'eux-mêmes à 1 chaque jour (séquence par service et par jour).
 */
@Component
public class DailyMaintenance {

	private static final Logger log = LoggerFactory.getLogger(DailyMaintenance.class);

	private final TicketRepository tickets;
	private final PushSubscriptionRepository pushSubscriptions;
	private final Clock clock;

	public DailyMaintenance(TicketRepository tickets, PushSubscriptionRepository pushSubscriptions, Clock clock) {
		this.tickets = tickets;
		this.pushSubscriptions = pushSubscriptions;
		this.clock = clock;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(cron = "0 1 0 * * *", zone = "${app.timezone}")
	@Transactional
	public void expireStaleTickets() {
		int expired = tickets.expireBefore(LocalDate.now(clock));
		if (expired > 0) {
			log.info("{} ticket(s) des jours précédents marqués expirés", expired);
		}
		// Un ticket ne vit qu'une journée : ses abonnements push ne servent plus au-delà.
		pushSubscriptions.deleteOlderThan(Instant.now(clock).minus(Duration.ofDays(2)));
	}
}

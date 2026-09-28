package io.noba.service;

import io.noba.domain.QueueService;
import io.noba.domain.Ticket;
import io.noba.repo.CounterRepository;
import io.noba.repo.TicketRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Estimation du temps d'attente :
 * personnes devant × durée moyenne de traitement ÷ nombre de guichets ouverts sur ce service.
 * La durée moyenne est glissante (derniers tickets terminés du jour) ; à défaut, la valeur configurée.
 */
@Component
public class WaitEstimator {

	private static final int SAMPLE_SIZE = 20;
	private static final int MIN_SAMPLES = 3;

	private final TicketRepository tickets;
	private final CounterRepository counters;

	public WaitEstimator(TicketRepository tickets, CounterRepository counters) {
		this.tickets = tickets;
		this.counters = counters;
	}

	public double avgServiceSeconds(QueueService service, LocalDate day) {
		List<Ticket> recent = tickets.findRecentDone(service.getId(), day, PageRequest.of(0, SAMPLE_SIZE));
		if (recent.size() < MIN_SAMPLES) {
			return service.getDefaultServiceMinutes() * 60.0;
		}
		return recent.stream()
				.mapToLong(t -> Duration.between(t.getCalledAt(), t.getCompletedAt()).toSeconds())
				.average()
				.orElse(service.getDefaultServiceMinutes() * 60.0);
	}

	/**
	 * Le « + 0,5 » compte en moyenne la moitié du client actuellement au guichet :
	 * même en tête de file, on n'est pas servi instantanément.
	 */
	public int estimateMinutes(QueueService service, LocalDate day, long peopleAhead) {
		long openCounters = Math.max(1, counters.countOpenForService(service.getId()));
		double seconds = (Math.max(0, peopleAhead) + 0.5) * avgServiceSeconds(service, day) / openCounters;
		return (int) Math.ceil(seconds / 60.0);
	}
}

package io.noba.service;

import io.noba.domain.Branch;
import io.noba.domain.Ticket;
import io.noba.domain.TicketStatus;
import io.noba.repo.BranchRepository;
import io.noba.repo.TicketRepository;
import io.noba.security.CurrentUser;
import io.noba.web.ApiException;
import io.noba.web.dto.AdminDtos.AgentStats;
import io.noba.web.dto.AdminDtos.ServiceStats;
import io.noba.web.dto.AdminDtos.StatsView;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicateurs d'un établissement sur une période (calculés en mémoire : suffisant pour le MVP). */
@Service
public class StatsService {

	private static final int MAX_DAYS = 92;

	private final BranchRepository branches;
	private final TicketRepository tickets;
	private final Clock clock;

	public StatsService(BranchRepository branches, TicketRepository tickets, Clock clock) {
		this.branches = branches;
		this.tickets = tickets;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public StatsView stats(CurrentUser user, Long branchId, LocalDate from, LocalDate to) {
		Branch branch = branches.findById(branchId).orElseThrow(() -> ApiException.notFound("Établissement introuvable."));
		user.checkBranch(branch);
		if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
			throw ApiException.badRequest("Période invalide (" + MAX_DAYS + " jours maximum).");
		}

		List<Ticket> all = tickets.findForStats(branchId, from, to);
		List<Ticket> served = all.stream().filter(t -> t.getStatus() == TicketStatus.DONE).toList();

		List<Long> byHour = new ArrayList<>(java.util.Collections.nCopies(24, 0L));
		all.forEach(t -> {
			int hour = t.getCreatedAt().atZone(clock.getZone()).getHour();
			byHour.set(hour, byHour.get(hour) + 1);
		});

		Map<String, List<Ticket>> perService = all.stream()
				.collect(Collectors.groupingBy(t -> t.getService().getName(), LinkedHashMap::new, Collectors.toList()));
		List<ServiceStats> byService = perService.entrySet().stream()
				.map(e -> new ServiceStats(e.getKey(), e.getValue().size(),
						e.getValue().stream().filter(t -> t.getStatus() == TicketStatus.DONE).count(),
						avgWait(e.getValue())))
				.sorted(Comparator.comparingLong(ServiceStats::total).reversed())
				.toList();

		Map<String, List<Ticket>> perAgent = served.stream()
				.filter(t -> t.getAgent() != null)
				.collect(Collectors.groupingBy(t -> t.getAgent().getFullName()));
		List<AgentStats> byAgent = perAgent.entrySet().stream()
				.map(e -> new AgentStats(e.getKey(), e.getValue().size(), avgService(e.getValue())))
				.sorted(Comparator.comparingLong(AgentStats::served).reversed())
				.toList();

		List<Integer> ratings = all.stream().map(Ticket::getRating).filter(java.util.Objects::nonNull).toList();

		return new StatsView(all.size(), served.size(), count(all, TicketStatus.NO_SHOW), count(all, TicketStatus.CANCELLED),
				count(all, TicketStatus.WAITING), avgWait(all), avgService(served),
				round(ratings.stream().mapToInt(Integer::intValue).average()), ratings.size(),
				byHour, byService, byAgent);
	}

	/** Attente = de la prise du ticket à l'appel. */
	private static Double avgWait(List<Ticket> list) {
		return round(list.stream().filter(t -> t.getCalledAt() != null)
				.mapToLong(t -> Duration.between(t.getCreatedAt(), t.getCalledAt()).toSeconds())
				.average(), 60);
	}

	/** Traitement = de l'appel à la clôture. */
	private static Double avgService(List<Ticket> list) {
		return round(list.stream().filter(t -> t.getCalledAt() != null && t.getCompletedAt() != null)
				.mapToLong(t -> Duration.between(t.getCalledAt(), t.getCompletedAt()).toSeconds())
				.average(), 60);
	}

	private static long count(List<Ticket> list, TicketStatus status) {
		return list.stream().filter(t -> t.getStatus() == status).count();
	}

	private static Double round(OptionalDouble value) {
		return round(value, 1);
	}

	private static Double round(OptionalDouble value, double divisor) {
		return value.isPresent() ? Math.round(value.getAsDouble() / divisor * 10) / 10.0 : null;
	}
}

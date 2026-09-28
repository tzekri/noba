package io.noba.service;

import io.noba.domain.Branch;
import io.noba.domain.Counter;
import io.noba.domain.QueueService;
import io.noba.domain.Role;
import io.noba.domain.StaffUser;
import io.noba.domain.Ticket;
import io.noba.domain.TicketStatus;
import io.noba.realtime.QueueEvent;
import io.noba.realtime.RealtimeHub;
import io.noba.repo.BranchRepository;
import io.noba.repo.CounterRepository;
import io.noba.repo.QueueServiceRepository;
import io.noba.repo.StaffUserRepository;
import io.noba.repo.TicketRepository;
import io.noba.security.CurrentUser;
import io.noba.web.ApiException;
import io.noba.web.dto.AgentDtos.AgentTicket;
import io.noba.web.dto.AgentDtos.BoardView;
import io.noba.web.dto.AgentDtos.BranchSummary;
import io.noba.web.dto.AgentDtos.CounterView;
import io.noba.web.dto.AgentDtos.ServiceBoard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Côté guichet : ouvrir un guichet, appeler le suivant, rappeler, terminer, absent, transférer. */
@Service
@Transactional
public class CounterDeskService {

	private static final int WAITING_LIST_SIZE = 50;

	private final BranchRepository branches;
	private final CounterRepository counters;
	private final QueueServiceRepository services;
	private final TicketRepository tickets;
	private final StaffUserRepository staff;
	private final WaitEstimator estimator;
	private final RealtimeHub hub;
	private final Clock clock;

	public CounterDeskService(BranchRepository branches, CounterRepository counters, QueueServiceRepository services,
			TicketRepository tickets, StaffUserRepository staff, WaitEstimator estimator, RealtimeHub hub, Clock clock) {
		this.branches = branches;
		this.counters = counters;
		this.services = services;
		this.tickets = tickets;
		this.staff = staff;
		this.estimator = estimator;
		this.hub = hub;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<BranchSummary> branches(CurrentUser user) {
		List<Branch> list = user.role() == Role.AGENT && user.branchId() != null
				? branches.findById(user.branchId()).stream().toList()
				: branches.findByOrganizationIdOrderByName(user.organizationId());
		return list.stream().map(b -> new BranchSummary(b.getId(), b.getName(), b.getCode(), b.isOpen())).toList();
	}

	@Transactional(readOnly = true)
	public BoardView board(CurrentUser user, Long branchId) {
		Branch branch = branches.findById(branchId).orElseThrow(() -> ApiException.notFound("Établissement introuvable."));
		user.checkBranch(branch);
		LocalDate today = today();

		List<ServiceBoard> serviceBoards = services.findByBranchIdAndActiveTrueOrderBySortOrderAscNameAsc(branchId).stream()
				.map(s -> {
					long waiting = tickets.countByServiceIdAndDayAndStatus(s.getId(), today, TicketStatus.WAITING);
					return new ServiceBoard(s.getId(), s.getName(), s.getPrefix(), waiting,
							estimator.estimateMinutes(s, today, waiting),
							(int) Math.round(estimator.avgServiceSeconds(s, today) / 60.0));
				})
				.toList();

		List<CounterView> counterViews = counters.findByBranchIdOrderByName(branchId).stream()
				.filter(Counter::isActive)
				.map(this::counterView)
				.toList();

		List<AgentTicket> waiting = tickets.findWaiting(branchId, today, PageRequest.of(0, WAITING_LIST_SIZE)).stream()
				.map(Views::agentTicket)
				.toList();

		var recent = tickets.findRecentlyCalled(branchId, today,
						EnumSet.of(TicketStatus.DONE, TicketStatus.NO_SHOW), PageRequest.of(0, 10)).stream()
				.map(Views::called)
				.toList();

		return new BoardView(new BranchSummary(branch.getId(), branch.getName(), branch.getCode(), branch.isOpen()),
				serviceBoards, counterViews, waiting, recent);
	}

	/** Un agent n'occupe qu'un guichet à la fois : ouvrir un guichet libère le précédent. */
	public CounterView open(CurrentUser user, Long counterId) {
		Counter counter = findCounter(user, counterId);
		StaffUser me = staff.getReferenceById(user.userId());
		if (counter.isOpen() && !counter.getCurrentAgent().getId().equals(user.userId())) {
			throw ApiException.conflict("Ce guichet est déjà occupé par " + counter.getCurrentAgent().getFullName() + ".");
		}
		counters.findByCurrentAgentId(user.userId())
				.filter(other -> !other.getId().equals(counterId))
				.ifPresent(other -> other.setCurrentAgent(null));
		counter.setCurrentAgent(me);
		hub.publishAfterCommit(counter.getBranch().getId(), QueueEvent.of("COUNTER"));
		return counterView(counter);
	}

	public CounterView close(CurrentUser user, Long counterId) {
		Counter counter = findCounter(user, counterId);
		counter.setCurrentAgent(null);
		hub.publishAfterCommit(counter.getBranch().getId(), QueueEvent.of("COUNTER"));
		return counterView(counter);
	}

	/**
	 * Termine le ticket en cours au guichet puis appelle le plus ancien ticket en attente parmi
	 * les services du guichet. Renvoie null si personne n'attend.
	 */
	public AgentTicket callNext(CurrentUser user, Long counterId) {
		Counter counter = findCounter(user, counterId);
		if (!counter.isOpen() || !counter.getCurrentAgent().getId().equals(user.userId())) {
			throw ApiException.conflict("Ouvrez d'abord ce guichet.");
		}
		Set<Long> serviceIds = counter.getServices().stream()
				.filter(QueueService::isActive)
				.map(QueueService::getId)
				.collect(Collectors.toSet());
		if (serviceIds.isEmpty()) {
			throw ApiException.badRequest("Aucun service n'est affecté à ce guichet.");
		}

		Instant now = Instant.now(clock);
		for (Ticket current : tickets.findCurrentAtCounter(counterId)) {
			current.setStatus(TicketStatus.DONE);
			current.setCompletedAt(now);
		}

		List<Ticket> next = tickets.lockNextWaiting(serviceIds, today(), PageRequest.of(0, 1));
		if (next.isEmpty()) {
			hub.publishAfterCommit(counter.getBranch().getId(), QueueEvent.of("COMPLETED"));
			return null;
		}
		Ticket ticket = next.get(0);
		ticket.setStatus(TicketStatus.CALLED);
		ticket.setCalledAt(now);
		ticket.setCounter(counter);
		ticket.setAgent(counter.getCurrentAgent());
		hub.publishAfterCommit(counter.getBranch().getId(), new QueueEvent("CALLED", ticket.getCode(), counter.getName()));
		return Views.agentTicket(ticket);
	}

	public AgentTicket recall(CurrentUser user, Long ticketId) {
		Ticket ticket = findTicket(user, ticketId, EnumSet.of(TicketStatus.CALLED));
		ticket.setRecallCount(ticket.getRecallCount() + 1);
		hub.publishAfterCommit(ticket.getBranch().getId(),
				new QueueEvent("RECALLED", ticket.getCode(), ticket.getCounter().getName()));
		return Views.agentTicket(ticket);
	}

	public AgentTicket start(CurrentUser user, Long ticketId) {
		Ticket ticket = findTicket(user, ticketId, EnumSet.of(TicketStatus.CALLED));
		ticket.setStatus(TicketStatus.SERVING);
		ticket.setStartedAt(Instant.now(clock));
		hub.publishAfterCommit(ticket.getBranch().getId(), new QueueEvent("STARTED", ticket.getCode(), null));
		return Views.agentTicket(ticket);
	}

	public AgentTicket complete(CurrentUser user, Long ticketId) {
		Ticket ticket = findTicket(user, ticketId, EnumSet.of(TicketStatus.CALLED, TicketStatus.SERVING));
		ticket.setStatus(TicketStatus.DONE);
		ticket.setCompletedAt(Instant.now(clock));
		hub.publishAfterCommit(ticket.getBranch().getId(), new QueueEvent("COMPLETED", ticket.getCode(), null));
		return Views.agentTicket(ticket);
	}

	public AgentTicket noShow(CurrentUser user, Long ticketId) {
		Ticket ticket = findTicket(user, ticketId, EnumSet.of(TicketStatus.CALLED));
		ticket.setStatus(TicketStatus.NO_SHOW);
		ticket.setCompletedAt(Instant.now(clock));
		hub.publishAfterCommit(ticket.getBranch().getId(), new QueueEvent("NO_SHOW", ticket.getCode(), null));
		return Views.agentTicket(ticket);
	}

	/** Renvoie le ticket dans la file d'un autre service, à sa place d'origine (heure de prise conservée). */
	public AgentTicket transfer(CurrentUser user, Long ticketId, Long serviceId) {
		Ticket ticket = findTicket(user, ticketId, EnumSet.of(TicketStatus.WAITING, TicketStatus.CALLED, TicketStatus.SERVING));
		QueueService target = services.findById(serviceId)
				.filter(s -> s.getBranch().getId().equals(ticket.getBranch().getId()) && s.isActive())
				.orElseThrow(() -> ApiException.notFound("Service introuvable."));
		ticket.setService(target);
		ticket.setStatus(TicketStatus.WAITING);
		ticket.setCounter(null);
		ticket.setAgent(null);
		ticket.setCalledAt(null);
		ticket.setStartedAt(null);
		hub.publishAfterCommit(ticket.getBranch().getId(), new QueueEvent("TRANSFERRED", ticket.getCode(), null));
		return Views.agentTicket(ticket);
	}

	private CounterView counterView(Counter c) {
		AgentTicket current = tickets.findCurrentAtCounter(c.getId()).stream()
				.findFirst()
				.map(Views::agentTicket)
				.orElse(null);
		StaffUser agent = c.getCurrentAgent();
		return new CounterView(c.getId(), c.getName(), c.isActive(),
				agent == null ? null : agent.getId(), agent == null ? null : agent.getFullName(),
				c.getServices().stream().map(QueueService::getId).sorted().toList(), current);
	}

	private Counter findCounter(CurrentUser user, Long counterId) {
		Counter counter = counters.findById(counterId).orElseThrow(() -> ApiException.notFound("Guichet introuvable."));
		user.checkBranch(counter.getBranch());
		if (!counter.isActive()) {
			throw ApiException.conflict("Ce guichet est désactivé.");
		}
		return counter;
	}

	private Ticket findTicket(CurrentUser user, Long ticketId, Set<TicketStatus> allowed) {
		Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> ApiException.notFound("Ticket introuvable."));
		user.checkBranch(ticket.getBranch());
		if (!allowed.contains(ticket.getStatus())) {
			throw ApiException.conflict("Action impossible : le ticket " + ticket.getCode() + " est déjà " + label(ticket.getStatus()) + ".");
		}
		return ticket;
	}

	private static String label(TicketStatus status) {
		return switch (status) {
			case WAITING -> "en attente";
			case CALLED -> "appelé";
			case SERVING -> "en cours de traitement";
			case DONE -> "terminé";
			case NO_SHOW -> "marqué absent";
			case CANCELLED -> "annulé";
			case EXPIRED -> "expiré";
		};
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}
}

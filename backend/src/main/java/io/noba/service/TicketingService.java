package io.noba.service;

import io.noba.domain.Branch;
import io.noba.domain.QueueService;
import io.noba.domain.Ticket;
import io.noba.domain.TicketStatus;
import io.noba.realtime.QueueEvent;
import io.noba.realtime.RealtimeHub;
import io.noba.repo.BranchRepository;
import io.noba.repo.QueueServiceRepository;
import io.noba.repo.TicketRepository;
import io.noba.web.ApiException;
import io.noba.web.dto.PublicDtos.BranchPublicView;
import io.noba.web.dto.PublicDtos.CalledTicket;
import io.noba.web.dto.PublicDtos.DisplayView;
import io.noba.web.dto.PublicDtos.ServicePublicView;
import io.noba.web.dto.PublicDtos.ServiceQueue;
import io.noba.web.dto.PublicDtos.TicketView;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Côté client : prise de ticket, suivi, annulation, avis — et écran d'affichage. */
@Service
public class TicketingService {

	private static final int DISPLAY_SIZE = 8;

	private final BranchRepository branches;
	private final QueueServiceRepository services;
	private final TicketRepository tickets;
	private final TicketNumberAllocator numbers;
	private final WaitEstimator estimator;
	private final RealtimeHub hub;
	private final Clock clock;

	public TicketingService(BranchRepository branches, QueueServiceRepository services, TicketRepository tickets,
			TicketNumberAllocator numbers, WaitEstimator estimator, RealtimeHub hub, Clock clock) {
		this.branches = branches;
		this.services = services;
		this.tickets = tickets;
		this.numbers = numbers;
		this.estimator = estimator;
		this.hub = hub;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public BranchPublicView branch(String code) {
		Branch branch = findBranch(code);
		LocalDate today = today();
		boolean accepting = accepting(branch);
		List<ServicePublicView> list = services.findByBranchIdAndActiveTrueOrderBySortOrderAscNameAsc(branch.getId())
				.stream()
				.map(s -> {
					long waiting = tickets.countByServiceIdAndDayAndStatus(s.getId(), today, TicketStatus.WAITING);
					boolean available = accepting && (s.getDailyLimit() == null
							|| tickets.countByServiceIdAndDay(s.getId(), today) < s.getDailyLimit());
					return new ServicePublicView(s.getId(), s.getName(), s.getDescription(), s.getPrefix(), waiting,
							estimator.estimateMinutes(s, today, waiting), available);
				})
				.toList();
		return new BranchPublicView(branch.getCode(), branch.getName(), branch.getOrganization().getName(),
				branch.getAddress(), accepting, list);
	}

	@Transactional
	public TicketView take(String branchCode, Long serviceId) {
		Branch branch = findBranch(branchCode);
		if (!accepting(branch)) {
			throw ApiException.conflict("Cet établissement ne délivre pas de tickets pour le moment.");
		}
		QueueService service = services.findById(serviceId)
				.filter(s -> s.getBranch().getId().equals(branch.getId()) && s.isActive())
				.orElseThrow(() -> ApiException.notFound("Service introuvable."));
		LocalDate today = today();
		if (service.getDailyLimit() != null
				&& tickets.countByServiceIdAndDay(service.getId(), today) >= service.getDailyLimit()) {
			throw ApiException.conflict("Plus de tickets disponibles aujourd'hui pour ce service.");
		}

		numbers.ensureSequence(service.getId(), today);
		int number = numbers.next(service.getId(), today);
		Ticket ticket = tickets.save(new Ticket(branch, service, today, number, UUID.randomUUID().toString(), Instant.now(clock)));

		hub.publishAfterCommit(branch.getId(), new QueueEvent("ISSUED", ticket.getCode(), null));
		return view(ticket);
	}

	@Transactional(readOnly = true)
	public TicketView ticket(String token) {
		return view(findTicket(token));
	}

	@Transactional
	public TicketView cancel(String token) {
		Ticket ticket = findTicket(token);
		if (ticket.getStatus() != TicketStatus.WAITING) {
			throw ApiException.conflict("Ce ticket ne peut plus être annulé.");
		}
		ticket.setStatus(TicketStatus.CANCELLED);
		ticket.setCompletedAt(Instant.now(clock));
		hub.publishAfterCommit(ticket.getBranch().getId(), new QueueEvent("CANCELLED", ticket.getCode(), null));
		return view(ticket);
	}

	@Transactional
	public TicketView rate(String token, int score) {
		Ticket ticket = findTicket(token);
		if (ticket.getStatus() != TicketStatus.DONE) {
			throw ApiException.conflict("Vous pourrez donner votre avis une fois servi.");
		}
		if (ticket.getRating() != null) {
			throw ApiException.conflict("Merci, votre avis a déjà été enregistré.");
		}
		ticket.setRating(score);
		return view(ticket);
	}

	@Transactional(readOnly = true)
	public DisplayView display(String code) {
		Branch branch = findBranch(code);
		LocalDate today = today();
		List<CalledTicket> called = tickets.findRecentlyCalled(branch.getId(), today,
						EnumSet.of(TicketStatus.CALLED, TicketStatus.SERVING, TicketStatus.DONE), PageRequest.of(0, DISPLAY_SIZE))
				.stream()
				.map(Views::called)
				.toList();
		List<ServiceQueue> queues = services.findByBranchIdAndActiveTrueOrderBySortOrderAscNameAsc(branch.getId())
				.stream()
				.map(s -> new ServiceQueue(s.getName(), s.getPrefix(),
						tickets.countByServiceIdAndDayAndStatus(s.getId(), today, TicketStatus.WAITING)))
				.toList();
		return new DisplayView(branch.getName(), branch.getOrganization().getName(), called, queues);
	}

	public Long branchIdForStream(String code) {
		return findBranch(code).getId();
	}

	private TicketView view(Ticket t) {
		Integer ahead = null;
		Integer estimate = null;
		if (t.getStatus() == TicketStatus.WAITING) {
			long count = tickets.countAhead(t.getService().getId(), t.getDay(), t.getQueuedAt());
			ahead = (int) count;
			estimate = estimator.estimateMinutes(t.getService(), t.getDay(), count);
		}
		Branch b = t.getBranch();
		return new TicketView(t.getPublicToken(), t.getCode(), t.getStatus(), t.getService().getName(), b.getName(),
				b.getCode(), b.getOrganization().getName(), ahead, estimate,
				t.getCounter() == null ? null : t.getCounter().getName(), t.getCreatedAt(), t.getCalledAt(), t.getRating());
	}

	private boolean accepting(Branch branch) {
		return branch.isOpen() && branch.getOrganization().isActive();
	}

	private Branch findBranch(String code) {
		return branches.findByCode(code).orElseThrow(() -> ApiException.notFound("Établissement introuvable."));
	}

	private Ticket findTicket(String token) {
		return tickets.findByPublicToken(token).orElseThrow(() -> ApiException.notFound("Ticket introuvable."));
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}
}

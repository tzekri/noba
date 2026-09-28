package io.noba.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tickets", indexes = {
		@Index(name = "idx_ticket_queue", columnList = "service_id, ticket_day, status"),
		@Index(name = "idx_ticket_branch_day", columnList = "branch_id, ticket_day")
})
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Branch branch;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private QueueService service;

	@Column(name = "ticket_day", nullable = false)
	private LocalDate day;

	@Column(name = "seq_number", nullable = false)
	private int number;

	/** Numéro affiché, ex. « B-042 ». */
	@Column(nullable = false)
	private String code;

	/** Jeton secret de l'URL de suivi : le client n'a pas de compte. */
	@Column(nullable = false, unique = true, length = 36)
	private String publicToken;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TicketStatus status = TicketStatus.WAITING;

	/** Ordre dans la file. Conservé lors d'un transfert pour ne pas pénaliser le client. */
	@Column(nullable = false)
	private Instant queuedAt;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant calledAt;
	private Instant startedAt;
	private Instant completedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	private Counter counter;

	@ManyToOne(fetch = FetchType.LAZY)
	private StaffUser agent;

	@Column(nullable = false)
	private int recallCount;

	/** Satisfaction 1..5, saisie par le client après son passage. */
	private Integer rating;

	protected Ticket() {
	}

	public Ticket(Branch branch, QueueService service, LocalDate day, int number, String publicToken, Instant now) {
		this.branch = branch;
		this.service = service;
		this.day = day;
		this.number = number;
		this.code = service.getPrefix() + "-" + String.format("%03d", number);
		this.publicToken = publicToken;
		this.createdAt = now;
		this.queuedAt = now;
	}

	public Long getId() { return id; }
	public Branch getBranch() { return branch; }
	public QueueService getService() { return service; }
	public void setService(QueueService service) { this.service = service; }
	public LocalDate getDay() { return day; }
	public int getNumber() { return number; }
	public String getCode() { return code; }
	public String getPublicToken() { return publicToken; }
	public TicketStatus getStatus() { return status; }
	public void setStatus(TicketStatus status) { this.status = status; }
	public Instant getQueuedAt() { return queuedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getCalledAt() { return calledAt; }
	public void setCalledAt(Instant calledAt) { this.calledAt = calledAt; }
	public Instant getStartedAt() { return startedAt; }
	public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
	public Counter getCounter() { return counter; }
	public void setCounter(Counter counter) { this.counter = counter; }
	public StaffUser getAgent() { return agent; }
	public void setAgent(StaffUser agent) { this.agent = agent; }
	public int getRecallCount() { return recallCount; }
	public void setRecallCount(int recallCount) { this.recallCount = recallCount; }
	public Integer getRating() { return rating; }
	public void setRating(Integer rating) { this.rating = rating; }
}

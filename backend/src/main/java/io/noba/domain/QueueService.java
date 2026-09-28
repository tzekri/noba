package io.noba.domain;

import jakarta.persistence.*;

/** Un service = une file d'attente (ex. « Retrait », « Ouverture de compte »). */
@Entity
@Table(name = "queue_services")
public class QueueService {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Branch branch;

	@Column(nullable = false)
	private String name;

	private String description;

	/** Préfixe des tickets : A → A-001. */
	@Column(nullable = false, length = 3)
	private String prefix;

	/** Durée moyenne de traitement utilisée tant qu'il n'y a pas assez d'historique. */
	@Column(nullable = false)
	private int defaultServiceMinutes = 5;

	/** Nombre maximum de tickets par jour (null = illimité). */
	private Integer dailyLimit;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private int sortOrder;

	protected QueueService() {
	}

	public QueueService(Branch branch, String name, String prefix) {
		this.branch = branch;
		this.name = name;
		this.prefix = prefix;
	}

	public Long getId() { return id; }
	public Branch getBranch() { return branch; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getPrefix() { return prefix; }
	public void setPrefix(String prefix) { this.prefix = prefix; }
	public int getDefaultServiceMinutes() { return defaultServiceMinutes; }
	public void setDefaultServiceMinutes(int defaultServiceMinutes) { this.defaultServiceMinutes = defaultServiceMinutes; }
	public Integer getDailyLimit() { return dailyLimit; }
	public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public int getSortOrder() { return sortOrder; }
	public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

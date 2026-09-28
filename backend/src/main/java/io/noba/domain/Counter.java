package io.noba.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/** Un guichet. Il traite un ou plusieurs services ; un agent l'ouvre pour appeler des tickets. */
@Entity
@Table(name = "counters")
public class Counter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Branch branch;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private boolean active = true;

	@ManyToMany
	@JoinTable(name = "counter_services",
			joinColumns = @JoinColumn(name = "counter_id"),
			inverseJoinColumns = @JoinColumn(name = "service_id"))
	private Set<QueueService> services = new HashSet<>();

	/** Agent actuellement au guichet (null = guichet fermé). */
	@ManyToOne(fetch = FetchType.LAZY)
	private StaffUser currentAgent;

	protected Counter() {
	}

	public Counter(Branch branch, String name) {
		this.branch = branch;
		this.name = name;
	}

	public boolean isOpen() {
		return currentAgent != null;
	}

	public Long getId() { return id; }
	public Branch getBranch() { return branch; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public Set<QueueService> getServices() { return services; }
	public StaffUser getCurrentAgent() { return currentAgent; }
	public void setCurrentAgent(StaffUser currentAgent) { this.currentAgent = currentAgent; }
}

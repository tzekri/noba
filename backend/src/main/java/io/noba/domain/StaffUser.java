package io.noba.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Membre du personnel (agent, responsable) ou opérateur de la plateforme. Les clients n'ont pas de compte. */
@Entity
@Table(name = "staff_users")
public class StaffUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Null uniquement pour SUPER_ADMIN. */
	@ManyToOne(fetch = FetchType.LAZY)
	private Organization organization;

	/** Établissement de rattachement d'un AGENT (null pour ORG_ADMIN / SUPER_ADMIN). */
	@ManyToOne(fetch = FetchType.LAZY)
	private Branch branch;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(name = "staff_role", nullable = false)
	private Role role;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected StaffUser() {
	}

	public StaffUser(Organization organization, Branch branch, String email, String passwordHash, String fullName, Role role) {
		this.organization = organization;
		this.branch = branch;
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.role = role;
	}

	public Long getId() { return id; }
	public Organization getOrganization() { return organization; }
	public Branch getBranch() { return branch; }
	public void setBranch(Branch branch) { this.branch = branch; }
	public String getEmail() { return email; }
	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public Role getRole() { return role; }
	public void setRole(Role role) { this.role = role; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public Instant getCreatedAt() { return createdAt; }
}

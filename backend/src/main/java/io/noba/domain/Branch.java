package io.noba.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Un établissement physique (agence, clinique, mairie…) où les clients prennent un ticket. */
@Entity
@Table(name = "branches")
public class Branch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Organization organization;

	@Column(nullable = false)
	private String name;

	/** Code public utilisé dans l'URL du QR code : /q/{code}. */
	@Column(nullable = false, unique = true)
	private String code;

	private String address;

	/** Accepte-t-il de nouveaux tickets ? */
	@Column(name = "is_open", nullable = false)
	private boolean open = true;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected Branch() {
	}

	public Branch(Organization organization, String name, String code, String address) {
		this.organization = organization;
		this.name = name;
		this.code = code;
		this.address = address;
	}

	public Long getId() { return id; }
	public Organization getOrganization() { return organization; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getCode() { return code; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	public boolean isOpen() { return open; }
	public void setOpen(boolean open) { this.open = open; }
	public Instant getCreatedAt() { return createdAt; }
}

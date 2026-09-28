package io.noba.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Abonnement Web Push d'un navigateur, rattaché à un ticket : permet de prévenir le client page fermée. */
@Entity
@Table(name = "push_subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "endpoint"}))
public class PushSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Ticket ticket;

	/** URL du service push du navigateur (Google, Mozilla, Apple…). */
	@Column(nullable = false, length = 1024)
	private String endpoint;

	/** Clé publique ECDH du navigateur (Base64url). */
	@Column(nullable = false, length = 128)
	private String p256dh;

	/** Secret d'authentification du navigateur (Base64url). */
	@Column(nullable = false, length = 64)
	private String auth;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected PushSubscription() {
	}

	public PushSubscription(Ticket ticket, String endpoint, String p256dh, String auth) {
		this.ticket = ticket;
		this.endpoint = endpoint;
		this.p256dh = p256dh;
		this.auth = auth;
	}

	public Long getId() { return id; }
	public Ticket getTicket() { return ticket; }
	public String getEndpoint() { return endpoint; }
	public String getP256dh() { return p256dh; }
	public void setP256dh(String p256dh) { this.p256dh = p256dh; }
	public String getAuth() { return auth; }
	public void setAuth(String auth) { this.auth = auth; }
}

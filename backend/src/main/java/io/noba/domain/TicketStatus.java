package io.noba.domain;

public enum TicketStatus {
	WAITING,
	CALLED,
	SERVING,
	DONE,
	NO_SHOW,
	CANCELLED,
	/** Ticket non traité à la fermeture de la journée. */
	EXPIRED;

	public boolean isActive() {
		return this == WAITING || this == CALLED || this == SERVING;
	}
}

package io.noba.service;

import io.noba.domain.Ticket;
import io.noba.web.dto.AgentDtos.AgentTicket;
import io.noba.web.dto.PublicDtos.CalledTicket;

/** Conversions entité → DTO partagées entre services (à appeler dans une transaction). */
final class Views {

	private Views() {
	}

	static CalledTicket called(Ticket t) {
		return new CalledTicket(t.getCode(), t.getCounter() == null ? null : t.getCounter().getName(), t.getStatus(),
				t.getCalledAt());
	}

	static AgentTicket agentTicket(Ticket t) {
		return new AgentTicket(t.getId(), t.getCode(), t.getStatus(), t.getService().getId(), t.getService().getName(),
				t.getQueuedAt(), t.getCalledAt(), t.getStartedAt(), t.getRecallCount());
	}
}

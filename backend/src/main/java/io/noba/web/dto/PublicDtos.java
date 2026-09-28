package io.noba.web.dto;

import io.noba.domain.TicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Données exposées sans authentification : page client, suivi de ticket, écran d'affichage. */
public final class PublicDtos {

	private PublicDtos() {
	}

	public record BranchPublicView(String code, String name, String organizationName, String address,
			boolean open, List<ServicePublicView> services) {
	}

	public record ServicePublicView(Long id, String name, String description, String prefix,
			long waiting, int estimatedWaitMinutes, boolean available) {
	}

	public record TakeTicketRequest(@NotNull Long serviceId) {
	}

	/**
	 * @param peopleAhead personnes devant le client (null si le ticket n'est plus en attente)
	 */
	public record TicketView(String token, String code, TicketStatus status, String serviceName,
			String branchName, String branchCode, String organizationName, Integer peopleAhead,
			Integer estimatedWaitMinutes, String counterName, Instant createdAt, Instant calledAt, Integer rating) {
	}

	public record RatingRequest(@Min(1) @Max(5) int score) {
	}

	public record DisplayView(String branchName, String organizationName, List<CalledTicket> called,
			List<ServiceQueue> queues) {
	}

	public record CalledTicket(String code, String counterName, TicketStatus status, Instant calledAt) {
	}

	public record ServiceQueue(String name, String prefix, long waiting) {
	}
}

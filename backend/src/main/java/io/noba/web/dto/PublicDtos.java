package io.noba.web.dto;

import io.noba.domain.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
	 * @param peopleAhead  personnes devant le client (null si le ticket n'est plus en attente)
	 * @param recoveryCode code à 4 chiffres pour retrouver le ticket depuis un autre appareil
	 */
	public record TicketView(String token, String code, TicketStatus status, String serviceName,
			String branchName, String branchCode, String organizationName, Integer peopleAhead,
			Integer estimatedWaitMinutes, String counterName, Instant createdAt, Instant calledAt, Integer rating,
			String recoveryCode) {
	}

	/** « J'ai déjà un ticket » : numéro affiché (B-042, b42…) + code de récupération. */
	public record RecoverRequest(@NotBlank @Size(max = 10) String ticketCode,
			@NotBlank @Pattern(regexp = "\\d{4}", message = "4 chiffres") String recoveryCode) {
	}

	/** Abonnement Web Push tel que fourni par PushSubscription.toJSON() dans le navigateur. */
	public record PushSubscribeRequest(@NotBlank @Size(max = 1024) String endpoint, @NotNull @Valid PushKeys keys) {
	}

	public record PushKeys(@NotBlank @Size(max = 128) String p256dh, @NotBlank @Size(max = 64) String auth) {
	}

	public record PushUnsubscribeRequest(@NotBlank @Size(max = 1024) String endpoint) {
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

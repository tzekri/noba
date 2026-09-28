package io.noba.web.dto;

import io.noba.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	/** Inscription en libre-service d'une nouvelle organisation cliente (modèle SaaS). */
	public record RegisterRequest(@NotBlank @Size(max = 120) String organizationName,
			@NotBlank @Size(max = 120) String fullName,
			@NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 100) String password) {
	}

	public record AuthResponse(String token, MeView user) {
	}

	public record MeView(Long id, String fullName, String email, Role role, Long organizationId,
			String organizationName, Long branchId) {
	}
}

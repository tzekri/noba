package io.noba.web.dto;

import io.noba.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Back-office du responsable d'organisation, et console super-admin. */
public final class AdminDtos {

	private AdminDtos() {
	}

	public record BranchRequest(@NotBlank @Size(max = 120) String name, @Size(max = 255) String address, Boolean open) {
	}

	public record BranchAdminView(Long id, String name, String code, String address, boolean open) {
	}

	public record ServiceRequest(@NotBlank @Size(max = 120) String name,
			@Size(max = 255) String description,
			@NotBlank @Pattern(regexp = "[A-Z]{1,3}", message = "1 à 3 lettres majuscules") String prefix,
			@Min(1) @Max(240) int defaultServiceMinutes,
			@Min(1) Integer dailyLimit,
			boolean active,
			int sortOrder) {
	}

	public record ServiceAdminView(Long id, String name, String description, String prefix,
			int defaultServiceMinutes, Integer dailyLimit, boolean active, int sortOrder) {
	}

	public record CounterRequest(@NotBlank @Size(max = 60) String name, boolean active, List<Long> serviceIds) {
	}

	public record CounterAdminView(Long id, String name, boolean active, List<Long> serviceIds, String agentName) {
	}

	/** Le mot de passe est obligatoire à la création, facultatif en modification. */
	public record StaffRequest(@NotBlank @Size(max = 120) String fullName,
			@NotBlank @Email String email,
			@Size(min = 8, max = 100) String password,
			@NotNull Role role,
			Long branchId,
			Boolean active) {
	}

	public record StaffView(Long id, String fullName, String email, Role role, Long branchId, String branchName,
			boolean active) {
	}

	public record StatsView(long total, long served, long noShow, long cancelled, long waiting,
			Double avgWaitMinutes, Double avgServiceMinutes, Double avgRating, long ratings,
			List<Long> byHour, List<ServiceStats> byService, List<AgentStats> byAgent) {
	}

	public record ServiceStats(String name, long total, long served, Double avgWaitMinutes) {
	}

	public record AgentStats(String name, long served, Double avgServiceMinutes) {
	}

	public record OrganizationView(Long id, String name, String slug, boolean active, Instant createdAt,
			long branches, long staff) {
	}

	public record ActiveRequest(boolean active) {
	}
}

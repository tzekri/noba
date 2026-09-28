package io.noba.security;

import io.noba.domain.Branch;
import io.noba.domain.Role;
import io.noba.web.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Identité du membre du personnel connecté, extraite du JWT.
 * Sert aussi de garde multi-tenant : chaque accès à un établissement passe par {@link #checkBranch}.
 */
public record CurrentUser(Long userId, Role role, Long organizationId, Long branchId) {

	public static CurrentUser get() {
		if (!(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt)) {
			throw ApiException.forbidden();
		}
		return new CurrentUser(
				Long.valueOf(jwt.getSubject()),
				Role.valueOf(jwt.getClaimAsString("role")),
				asLong(jwt.getClaim("orgId")),
				asLong(jwt.getClaim("branchId")));
	}

	/** Vérifie que l'établissement appartient à l'organisation de l'utilisateur (et à l'agent, s'il y est rattaché). */
	public void checkBranch(Branch branch) {
		if (organizationId == null || !organizationId.equals(branch.getOrganization().getId())) {
			throw ApiException.forbidden();
		}
		if (role == Role.AGENT && branchId != null && !branchId.equals(branch.getId())) {
			throw ApiException.forbidden();
		}
	}

	private static Long asLong(Object claim) {
		return claim == null ? null : ((Number) claim).longValue();
	}
}

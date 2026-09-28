package io.noba.service;

import io.noba.domain.Branch;
import io.noba.domain.Counter;
import io.noba.domain.Organization;
import io.noba.domain.QueueService;
import io.noba.domain.Role;
import io.noba.domain.StaffUser;
import io.noba.repo.BranchRepository;
import io.noba.repo.CounterRepository;
import io.noba.repo.OrganizationRepository;
import io.noba.repo.QueueServiceRepository;
import io.noba.repo.StaffUserRepository;
import io.noba.security.TokenService;
import io.noba.web.ApiException;
import io.noba.web.dto.AuthDtos.AuthResponse;
import io.noba.web.dto.AuthDtos.LoginRequest;
import io.noba.web.dto.AuthDtos.MeView;
import io.noba.web.dto.AuthDtos.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final StaffUserRepository staff;
	private final OrganizationRepository organizations;
	private final BranchRepository branches;
	private final QueueServiceRepository services;
	private final CounterRepository counters;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokens;

	public AuthService(StaffUserRepository staff, OrganizationRepository organizations, BranchRepository branches,
			QueueServiceRepository services, CounterRepository counters, PasswordEncoder passwordEncoder,
			TokenService tokens) {
		this.staff = staff;
		this.organizations = organizations;
		this.branches = branches;
		this.services = services;
		this.counters = counters;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		StaffUser user = staff.findByEmailIgnoreCase(request.email().trim())
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou mot de passe incorrect."));
		if (!user.isActive() || (user.getOrganization() != null && !user.getOrganization().isActive())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Ce compte est désactivé.");
		}
		return new AuthResponse(tokens.issue(user), me(user));
	}

	/**
	 * Crée une organisation cliente, son responsable et un premier établissement prêt à l'emploi
	 * (un service « Accueil » et un guichet), pour pouvoir délivrer un ticket dès l'inscription.
	 */
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (staff.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Un compte existe déjà avec cet e-mail.");
		}
		String name = request.organizationName().trim();
		Organization org = organizations.save(new Organization(name, Slugs.uniqueCode(name, organizations::existsBySlug)));

		Branch branch = branches.save(new Branch(org, "Établissement principal",
				Slugs.uniqueCode(name, branches::existsByCode), null));
		QueueService reception = services.save(new QueueService(branch, "Accueil", "A"));
		Counter counter = new Counter(branch, "Guichet 1");
		counter.getServices().add(reception);
		counters.save(counter);

		StaffUser admin = staff.save(new StaffUser(org, null, email, passwordEncoder.encode(request.password()),
				request.fullName().trim(), Role.ORG_ADMIN));
		return new AuthResponse(tokens.issue(admin), me(admin));
	}

	@Transactional(readOnly = true)
	public MeView me(Long userId) {
		return me(staff.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expirée.")));
	}

	private MeView me(StaffUser u) {
		Organization org = u.getOrganization();
		return new MeView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
				org == null ? null : org.getId(), org == null ? null : org.getName(),
				u.getBranch() == null ? null : u.getBranch().getId());
	}
}

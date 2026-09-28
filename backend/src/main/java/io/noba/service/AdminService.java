package io.noba.service;

import io.noba.domain.Branch;
import io.noba.domain.Counter;
import io.noba.domain.Organization;
import io.noba.domain.QueueService;
import io.noba.domain.Role;
import io.noba.domain.StaffUser;
import io.noba.realtime.QueueEvent;
import io.noba.realtime.RealtimeHub;
import io.noba.repo.BranchRepository;
import io.noba.repo.CounterRepository;
import io.noba.repo.OrganizationRepository;
import io.noba.repo.QueueServiceRepository;
import io.noba.repo.StaffUserRepository;
import io.noba.security.CurrentUser;
import io.noba.web.ApiException;
import io.noba.web.dto.AdminDtos.BranchAdminView;
import io.noba.web.dto.AdminDtos.BranchRequest;
import io.noba.web.dto.AdminDtos.CounterAdminView;
import io.noba.web.dto.AdminDtos.CounterRequest;
import io.noba.web.dto.AdminDtos.ServiceAdminView;
import io.noba.web.dto.AdminDtos.ServiceRequest;
import io.noba.web.dto.AdminDtos.StaffRequest;
import io.noba.web.dto.AdminDtos.StaffView;
import java.util.HashSet;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuration d'une organisation par son responsable. Chaque méthode vérifie l'appartenance à l'organisation. */
@Service
@Transactional
public class AdminService {

	private final OrganizationRepository organizations;
	private final BranchRepository branches;
	private final QueueServiceRepository services;
	private final CounterRepository counters;
	private final StaffUserRepository staff;
	private final PasswordEncoder passwordEncoder;
	private final RealtimeHub hub;

	public AdminService(OrganizationRepository organizations, BranchRepository branches, QueueServiceRepository services,
			CounterRepository counters, StaffUserRepository staff, PasswordEncoder passwordEncoder, RealtimeHub hub) {
		this.organizations = organizations;
		this.branches = branches;
		this.services = services;
		this.counters = counters;
		this.staff = staff;
		this.passwordEncoder = passwordEncoder;
		this.hub = hub;
	}

	// ---- Établissements ----

	@Transactional(readOnly = true)
	public List<BranchAdminView> branches(CurrentUser user) {
		return branches.findByOrganizationIdOrderByName(user.organizationId()).stream().map(AdminService::view).toList();
	}

	public BranchAdminView createBranch(CurrentUser user, BranchRequest request) {
		Organization org = organizations.getReferenceById(user.organizationId());
		Branch branch = new Branch(org, request.name().trim(), Slugs.uniqueCode(request.name(), branches::existsByCode),
				blankToNull(request.address()));
		if (request.open() != null) {
			branch.setOpen(request.open());
		}
		return view(branches.save(branch));
	}

	public BranchAdminView updateBranch(CurrentUser user, Long id, BranchRequest request) {
		Branch branch = branch(user, id);
		branch.setName(request.name().trim());
		branch.setAddress(blankToNull(request.address()));
		if (request.open() != null) {
			branch.setOpen(request.open());
		}
		hub.publishAfterCommit(branch.getId(), QueueEvent.of("CONFIG"));
		return view(branch);
	}

	// ---- Services (files d'attente) ----

	@Transactional(readOnly = true)
	public List<ServiceAdminView> services(CurrentUser user, Long branchId) {
		branch(user, branchId);
		return services.findByBranchIdOrderBySortOrderAscNameAsc(branchId).stream().map(AdminService::view).toList();
	}

	public ServiceAdminView createService(CurrentUser user, Long branchId, ServiceRequest request) {
		Branch branch = branch(user, branchId);
		QueueService service = new QueueService(branch, request.name().trim(), request.prefix());
		apply(service, request);
		services.save(service);
		// Sans guichet, les tickets d'un service ne peuvent jamais être appelés.
		if (!Boolean.FALSE.equals(request.attachToAllCounters())) {
			counters.findByBranchIdOrderByName(branchId).stream()
					.filter(Counter::isActive)
					.forEach(c -> c.getServices().add(service));
		}
		hub.publishAfterCommit(branchId, QueueEvent.of("CONFIG"));
		return view(service);
	}

	public ServiceAdminView updateService(CurrentUser user, Long id, ServiceRequest request) {
		QueueService service = services.findById(id).orElseThrow(() -> ApiException.notFound("Service introuvable."));
		user.checkBranch(service.getBranch());
		service.setName(request.name().trim());
		apply(service, request);
		hub.publishAfterCommit(service.getBranch().getId(), QueueEvent.of("CONFIG"));
		return view(service);
	}

	private static void apply(QueueService service, ServiceRequest request) {
		service.setDescription(blankToNull(request.description()));
		service.setPrefix(request.prefix());
		service.setDefaultServiceMinutes(request.defaultServiceMinutes());
		service.setDailyLimit(request.dailyLimit());
		service.setActive(request.active());
		service.setSortOrder(request.sortOrder());
	}

	// ---- Guichets ----

	@Transactional(readOnly = true)
	public List<CounterAdminView> counters(CurrentUser user, Long branchId) {
		branch(user, branchId);
		return counters.findByBranchIdOrderByName(branchId).stream().map(AdminService::view).toList();
	}

	public CounterAdminView createCounter(CurrentUser user, Long branchId, CounterRequest request) {
		Branch branch = branch(user, branchId);
		Counter counter = new Counter(branch, request.name().trim());
		apply(counter, request);
		hub.publishAfterCommit(branchId, QueueEvent.of("CONFIG"));
		return view(counters.save(counter));
	}

	public CounterAdminView updateCounter(CurrentUser user, Long id, CounterRequest request) {
		Counter counter = counters.findById(id).orElseThrow(() -> ApiException.notFound("Guichet introuvable."));
		user.checkBranch(counter.getBranch());
		counter.setName(request.name().trim());
		apply(counter, request);
		if (!counter.isActive()) {
			counter.setCurrentAgent(null);
		}
		hub.publishAfterCommit(counter.getBranch().getId(), QueueEvent.of("CONFIG"));
		return view(counter);
	}

	private void apply(Counter counter, CounterRequest request) {
		counter.setActive(request.active());
		List<QueueService> selected = request.serviceIds() == null ? List.of() : services.findAllById(request.serviceIds());
		if (selected.stream().anyMatch(s -> !s.getBranch().getId().equals(counter.getBranch().getId()))) {
			throw ApiException.badRequest("Un service sélectionné n'appartient pas à cet établissement.");
		}
		counter.getServices().clear();
		counter.getServices().addAll(new HashSet<>(selected));
	}

	// ---- Personnel ----

	@Transactional(readOnly = true)
	public List<StaffView> staff(CurrentUser user) {
		return staff.findByOrganizationIdOrderByFullName(user.organizationId()).stream().map(AdminService::view).toList();
	}

	public StaffView createStaff(CurrentUser user, StaffRequest request) {
		if (request.password() == null || request.password().isBlank()) {
			throw ApiException.badRequest("Le mot de passe est obligatoire.");
		}
		String email = request.email().trim().toLowerCase();
		if (staff.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Un compte existe déjà avec cet e-mail.");
		}
		StaffUser member = new StaffUser(organizations.getReferenceById(user.organizationId()), null, email,
				passwordEncoder.encode(request.password()), request.fullName().trim(), Role.AGENT);
		applyRole(user, member, request);
		if (request.active() != null) {
			member.setActive(request.active());
		}
		return view(staff.save(member));
	}

	public StaffView updateStaff(CurrentUser user, Long id, StaffRequest request) {
		StaffUser member = staff.findById(id)
				.filter(m -> m.getOrganization() != null && m.getOrganization().getId().equals(user.organizationId()))
				.orElseThrow(() -> ApiException.notFound("Membre introuvable."));
		String email = request.email().trim().toLowerCase();
		if (!member.getEmail().equalsIgnoreCase(email)) {
			throw ApiException.badRequest("L'e-mail d'un compte ne peut pas être modifié.");
		}
		boolean self = member.getId().equals(user.userId());
		if (self && (request.role() != Role.ORG_ADMIN || Boolean.FALSE.equals(request.active()))) {
			throw ApiException.badRequest("Vous ne pouvez pas retirer vos propres droits d'administration.");
		}
		member.setFullName(request.fullName().trim());
		if (request.password() != null && !request.password().isBlank()) {
			member.setPasswordHash(passwordEncoder.encode(request.password()));
		}
		applyRole(user, member, request);
		if (request.active() != null) {
			member.setActive(request.active());
		}
		return view(member);
	}

	private void applyRole(CurrentUser user, StaffUser member, StaffRequest request) {
		switch (request.role()) {
			case AGENT -> {
				if (request.branchId() == null) {
					throw ApiException.badRequest("Un agent doit être rattaché à un établissement.");
				}
				member.setBranch(branch(user, request.branchId()));
			}
			case ORG_ADMIN -> member.setBranch(null);
			default -> throw ApiException.forbidden();
		}
		member.setRole(request.role());
	}

	// ---- Utilitaires ----

	private Branch branch(CurrentUser user, Long id) {
		Branch branch = branches.findById(id).orElseThrow(() -> ApiException.notFound("Établissement introuvable."));
		user.checkBranch(branch);
		return branch;
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private static BranchAdminView view(Branch b) {
		return new BranchAdminView(b.getId(), b.getName(), b.getCode(), b.getAddress(), b.isOpen());
	}

	private static ServiceAdminView view(QueueService s) {
		return new ServiceAdminView(s.getId(), s.getName(), s.getDescription(), s.getPrefix(),
				s.getDefaultServiceMinutes(), s.getDailyLimit(), s.isActive(), s.getSortOrder());
	}

	private static CounterAdminView view(Counter c) {
		return new CounterAdminView(c.getId(), c.getName(), c.isActive(),
				c.getServices().stream().map(QueueService::getId).sorted().toList(),
				c.getCurrentAgent() == null ? null : c.getCurrentAgent().getFullName());
	}

	private static StaffView view(StaffUser u) {
		Branch b = u.getBranch();
		return new StaffView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
				b == null ? null : b.getId(), b == null ? null : b.getName(), u.isActive());
	}
}

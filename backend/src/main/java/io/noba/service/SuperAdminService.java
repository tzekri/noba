package io.noba.service;

import io.noba.domain.Organization;
import io.noba.repo.BranchRepository;
import io.noba.repo.OrganizationRepository;
import io.noba.repo.StaffUserRepository;
import io.noba.web.ApiException;
import io.noba.web.dto.AdminDtos.OrganizationView;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Console de l'opérateur de la plateforme : liste et suspension des organisations clientes. */
@Service
@Transactional
public class SuperAdminService {

	private final OrganizationRepository organizations;
	private final BranchRepository branches;
	private final StaffUserRepository staff;

	public SuperAdminService(OrganizationRepository organizations, BranchRepository branches, StaffUserRepository staff) {
		this.organizations = organizations;
		this.branches = branches;
		this.staff = staff;
	}

	@Transactional(readOnly = true)
	public List<OrganizationView> organizations() {
		return organizations.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(this::view).toList();
	}

	public OrganizationView setActive(Long id, boolean active) {
		Organization org = organizations.findById(id).orElseThrow(() -> ApiException.notFound("Organisation introuvable."));
		org.setActive(active);
		return view(org);
	}

	private OrganizationView view(Organization o) {
		return new OrganizationView(o.getId(), o.getName(), o.getSlug(), o.isActive(), o.getCreatedAt(),
				branches.countByOrganizationId(o.getId()), staff.countByOrganizationId(o.getId()));
	}
}

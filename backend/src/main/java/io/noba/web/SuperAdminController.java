package io.noba.web;

import io.noba.service.SuperAdminService;
import io.noba.web.dto.AdminDtos.ActiveRequest;
import io.noba.web.dto.AdminDtos.OrganizationView;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super")
public class SuperAdminController {

	private final SuperAdminService superAdmin;

	public SuperAdminController(SuperAdminService superAdmin) {
		this.superAdmin = superAdmin;
	}

	@GetMapping("/organizations")
	public List<OrganizationView> organizations() {
		return superAdmin.organizations();
	}

	@PatchMapping("/organizations/{id}")
	public OrganizationView setActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
		return superAdmin.setActive(id, request.active());
	}
}

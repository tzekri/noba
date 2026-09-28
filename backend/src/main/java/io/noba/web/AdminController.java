package io.noba.web;

import io.noba.security.CurrentUser;
import io.noba.service.AdminService;
import io.noba.service.StatsService;
import io.noba.web.dto.AdminDtos.BranchAdminView;
import io.noba.web.dto.AdminDtos.BranchRequest;
import io.noba.web.dto.AdminDtos.CounterAdminView;
import io.noba.web.dto.AdminDtos.CounterRequest;
import io.noba.web.dto.AdminDtos.ServiceAdminView;
import io.noba.web.dto.AdminDtos.ServiceRequest;
import io.noba.web.dto.AdminDtos.StaffRequest;
import io.noba.web.dto.AdminDtos.StaffView;
import io.noba.web.dto.AdminDtos.StatsView;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminService admin;
	private final StatsService stats;

	public AdminController(AdminService admin, StatsService stats) {
		this.admin = admin;
		this.stats = stats;
	}

	@GetMapping("/branches")
	public List<BranchAdminView> branches() {
		return admin.branches(CurrentUser.get());
	}

	@PostMapping("/branches")
	@ResponseStatus(HttpStatus.CREATED)
	public BranchAdminView createBranch(@Valid @RequestBody BranchRequest request) {
		return admin.createBranch(CurrentUser.get(), request);
	}

	@PutMapping("/branches/{id}")
	public BranchAdminView updateBranch(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
		return admin.updateBranch(CurrentUser.get(), id, request);
	}

	@GetMapping("/branches/{id}/services")
	public List<ServiceAdminView> services(@PathVariable Long id) {
		return admin.services(CurrentUser.get(), id);
	}

	@PostMapping("/branches/{id}/services")
	@ResponseStatus(HttpStatus.CREATED)
	public ServiceAdminView createService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
		return admin.createService(CurrentUser.get(), id, request);
	}

	@PutMapping("/services/{id}")
	public ServiceAdminView updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
		return admin.updateService(CurrentUser.get(), id, request);
	}

	@GetMapping("/branches/{id}/counters")
	public List<CounterAdminView> counters(@PathVariable Long id) {
		return admin.counters(CurrentUser.get(), id);
	}

	@PostMapping("/branches/{id}/counters")
	@ResponseStatus(HttpStatus.CREATED)
	public CounterAdminView createCounter(@PathVariable Long id, @Valid @RequestBody CounterRequest request) {
		return admin.createCounter(CurrentUser.get(), id, request);
	}

	@PutMapping("/counters/{id}")
	public CounterAdminView updateCounter(@PathVariable Long id, @Valid @RequestBody CounterRequest request) {
		return admin.updateCounter(CurrentUser.get(), id, request);
	}

	@GetMapping("/staff")
	public List<StaffView> staff() {
		return admin.staff(CurrentUser.get());
	}

	@PostMapping("/staff")
	@ResponseStatus(HttpStatus.CREATED)
	public StaffView createStaff(@Valid @RequestBody StaffRequest request) {
		return admin.createStaff(CurrentUser.get(), request);
	}

	@PutMapping("/staff/{id}")
	public StaffView updateStaff(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
		return admin.updateStaff(CurrentUser.get(), id, request);
	}

	@GetMapping("/branches/{id}/stats")
	public StatsView stats(@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return stats.stats(CurrentUser.get(), id, from, to);
	}
}

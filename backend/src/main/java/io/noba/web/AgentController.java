package io.noba.web;

import io.noba.security.CurrentUser;
import io.noba.service.CounterDeskService;
import io.noba.web.dto.AgentDtos.AgentTicket;
import io.noba.web.dto.AgentDtos.BoardView;
import io.noba.web.dto.AgentDtos.BranchSummary;
import io.noba.web.dto.AgentDtos.CounterView;
import io.noba.web.dto.AgentDtos.TransferRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

	private final CounterDeskService desk;

	public AgentController(CounterDeskService desk) {
		this.desk = desk;
	}

	@GetMapping("/branches")
	public List<BranchSummary> branches() {
		return desk.branches(CurrentUser.get());
	}

	@GetMapping("/branches/{id}/board")
	public BoardView board(@PathVariable Long id) {
		return desk.board(CurrentUser.get(), id);
	}

	@PostMapping("/counters/{id}/open")
	public CounterView open(@PathVariable Long id) {
		return desk.open(CurrentUser.get(), id);
	}

	@PostMapping("/counters/{id}/close")
	public CounterView close(@PathVariable Long id) {
		return desk.close(CurrentUser.get(), id);
	}

	/** 204 si personne n'attend. */
	@PostMapping("/counters/{id}/next")
	public ResponseEntity<AgentTicket> next(@PathVariable Long id) {
		AgentTicket ticket = desk.callNext(CurrentUser.get(), id);
		return ticket == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(ticket);
	}

	@PostMapping("/tickets/{id}/recall")
	public AgentTicket recall(@PathVariable Long id) {
		return desk.recall(CurrentUser.get(), id);
	}

	@PostMapping("/tickets/{id}/start")
	public AgentTicket start(@PathVariable Long id) {
		return desk.start(CurrentUser.get(), id);
	}

	@PostMapping("/tickets/{id}/complete")
	public AgentTicket complete(@PathVariable Long id) {
		return desk.complete(CurrentUser.get(), id);
	}

	@PostMapping("/tickets/{id}/no-show")
	public AgentTicket noShow(@PathVariable Long id) {
		return desk.noShow(CurrentUser.get(), id);
	}

	@PostMapping("/tickets/{id}/transfer")
	public AgentTicket transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
		return desk.transfer(CurrentUser.get(), id, request.serviceId());
	}
}

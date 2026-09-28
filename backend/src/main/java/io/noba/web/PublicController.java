package io.noba.web;

import io.noba.realtime.RealtimeHub;
import io.noba.service.TicketingService;
import io.noba.web.dto.PublicDtos.BranchPublicView;
import io.noba.web.dto.PublicDtos.DisplayView;
import io.noba.web.dto.PublicDtos.RatingRequest;
import io.noba.web.dto.PublicDtos.TakeTicketRequest;
import io.noba.web.dto.PublicDtos.TicketView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** API sans authentification : le client est identifié par le jeton de son ticket. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

	private final TicketingService ticketing;
	private final RealtimeHub hub;

	public PublicController(TicketingService ticketing, RealtimeHub hub) {
		this.ticketing = ticketing;
		this.hub = hub;
	}

	@GetMapping("/branches/{code}")
	public BranchPublicView branch(@PathVariable String code) {
		return ticketing.branch(code);
	}

	@PostMapping("/branches/{code}/tickets")
	@ResponseStatus(HttpStatus.CREATED)
	public TicketView take(@PathVariable String code, @Valid @RequestBody TakeTicketRequest request) {
		return ticketing.take(code, request.serviceId());
	}

	@GetMapping("/branches/{code}/display")
	public DisplayView display(@PathVariable String code) {
		return ticketing.display(code);
	}

	@GetMapping(value = "/branches/{code}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@PathVariable String code) {
		return hub.subscribe(ticketing.branchIdForStream(code));
	}

	@GetMapping("/tickets/{token}")
	public TicketView ticket(@PathVariable String token) {
		return ticketing.ticket(token);
	}

	@PostMapping("/tickets/{token}/cancel")
	public TicketView cancel(@PathVariable String token) {
		return ticketing.cancel(token);
	}

	@PostMapping("/tickets/{token}/rating")
	public TicketView rate(@PathVariable String token, @Valid @RequestBody RatingRequest request) {
		return ticketing.rate(token, request.score());
	}
}

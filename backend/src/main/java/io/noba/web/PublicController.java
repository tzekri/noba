package io.noba.web;

import io.noba.realtime.RealtimeHub;
import io.noba.service.TicketingService;
import io.noba.web.dto.PublicDtos.BranchPublicView;
import io.noba.web.dto.PublicDtos.DisplayView;
import io.noba.web.dto.PublicDtos.RatingRequest;
import io.noba.web.dto.PublicDtos.RecoverRequest;
import io.noba.web.dto.PublicDtos.TakeTicketRequest;
import io.noba.web.dto.PublicDtos.TicketView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
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

	/** « J'ai déjà un ticket » : retrouve le suivi à partir du numéro et du code de récupération. */
	@PostMapping("/branches/{code}/tickets/recover")
	public TicketView recover(@PathVariable String code, @Valid @RequestBody RecoverRequest request,
			HttpServletRequest http) {
		return ticketing.recover(code, request.ticketCode(), request.recoveryCode(), http.getRemoteAddr());
	}

	@GetMapping("/branches/{code}/display")
	public DisplayView display(@PathVariable String code) {
		return ticketing.display(code);
	}

	@GetMapping(value = "/branches/{code}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@PathVariable String code, HttpServletResponse response) {
		// Empêche les proxys (Render, nginx…) de mettre le flux en mémoire tampon.
		response.setHeader("X-Accel-Buffering", "no");
		response.setHeader("Cache-Control", "no-cache");
		return hub.subscribe(ticketing.branchIdForStream(code));
	}

	/** Sonde de santé (Render, supervision). */
	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
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

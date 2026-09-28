package io.noba.web;

import io.noba.security.CurrentUser;
import io.noba.service.AuthService;
import io.noba.web.dto.AuthDtos.AuthResponse;
import io.noba.web.dto.AuthDtos.LoginRequest;
import io.noba.web.dto.AuthDtos.MeView;
import io.noba.web.dto.AuthDtos.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService auth;

	public AuthController(AuthService auth) {
		this.auth = auth;
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return auth.login(request);
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return auth.register(request);
	}

	@GetMapping("/me")
	public MeView me() {
		return auth.me(CurrentUser.get().userId());
	}
}

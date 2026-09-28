package io.noba.web;

import jakarta.persistence.OptimisticLockException;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
		return body(e.getStatus(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + " : " + f.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return body(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
	ResponseEntity<Map<String, Object>> handleConcurrent(Exception e) {
		return body(HttpStatus.CONFLICT, "Ce ticket vient d'être modifié par quelqu'un d'autre, réessayez.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<Map<String, Object>> handleDenied(AccessDeniedException e) {
		return body(HttpStatus.FORBIDDEN, "Accès refusé.");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Map<String, Object>> handleOther(Exception e) {
		log.error("Erreur inattendue", e);
		return body(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne, réessayez plus tard.");
	}

	private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
	}
}

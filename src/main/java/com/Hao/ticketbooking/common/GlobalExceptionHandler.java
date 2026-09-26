package com.Hao.ticketbooking.common;

import com.Hao.ticketbooking.auth.EmailAlreadyExistsException;
import com.Hao.ticketbooking.auth.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception thrown from a controller into a clean ErrorResponse.
 * Later phases add more specific handlers here (400, 409, 429...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailTaken(EmailAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("EMAIL_TAKEN", "Email is already registered"));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
	}

	// @PreAuthorize failures are thrown when the controller method is called, so they
	// arrive here instead of the security filter chain's 403 hook. Same body as that hook.
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErrorResponse("FORBIDDEN", "Access denied"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		// One entry per invalid field. These messages describe the client's own input,
		// so unlike unexpected errors they are safe to send back.
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
			fields.put(fe.getField(), fe.getDefaultMessage());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ValidationErrorResponse("VALIDATION_FAILED", "Request has invalid fields", fields));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
		// Spring's own exceptions (unknown URL, wrong HTTP method, ...) already
		// know their status. Keep it instead of turning them into a 500.
		if (ex instanceof org.springframework.web.ErrorResponse springError) {
			HttpStatusCode status = springError.getStatusCode();
			HttpStatus known = HttpStatus.resolve(status.value());
			String error = known != null ? known.name() : "ERROR";
			String message = known != null ? known.getReasonPhrase() : "Request failed";
			return ResponseEntity.status(status).body(new ErrorResponse(error, message));
		}

		// Anything else is a bug: log the details, but never send them to the client.
		log.error("Unexpected error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("INTERNAL_ERROR", "Something went wrong"));
	}
}

package com.spectrum.customer.rewards.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.spectrum.customer.rewards.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Translates exceptions thrown by the controllers and services into consistent
 * {@link ErrorResponse} bodies with the appropriate HTTP status.
 *
 * <p>Handler selection is by most specific exception type, so the catch-all
 * {@link #handleUnexpected(Exception, HttpServletRequest)} is only used when no other handler matches.</p>
 */
@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	/**
	 * Maps a missing resource (for example an unknown customer id) to {@code 404 Not Found}.
	 *
	 * @param ex      the exception raised by the service layer
	 * @param request the failing request, used to report the path
	 * @return the error response
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
	}

	/**
	 * Maps an invalid reporting period (from after to, period too long) to {@code 400 Bad Request}.
	 *
	 * @param ex      the exception raised by the service layer
	 * @param request the failing request, used to report the path
	 * @return the error response
	 */
	@ExceptionHandler(InvalidDateRangeException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRange(InvalidDateRangeException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
	}

	/**
	 * Maps Bean Validation failures on a request body to {@code 400 Bad Request}, listing every
	 * offending field in {@link ErrorResponse#getDetails()}.
	 *
	 * @param ex      the validation exception
	 * @param request the failing request, used to report the path
	 * @return the error response
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		// Sorted so the order of the messages is stable regardless of validator iteration order.
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Request validation failed", request, details);
	}

	/**
	 * Maps a request parameter or path variable that cannot be converted (for example a malformed
	 * date or a non numeric id) to {@code 400 Bad Request}.
	 *
	 * @param ex      the conversion exception
	 * @param request the failing request, used to report the path
	 * @return the error response
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST,
				"Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", request, null);
	}

	/**
	 * Maps a missing mandatory query parameter to {@code 400 Bad Request}.
	 *
	 * @param ex      the exception naming the missing parameter
	 * @param request the failing request, used to report the path
	 * @return the error response
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", request,
				null);
	}

	/**
	 * Maps an unparsable request body (for example malformed JSON) to {@code 400 Bad Request}.
	 * The parser's own message is not echoed back because it can leak internal class names.
	 *
	 * @param ex      the parsing exception
	 * @param request the failing request, used to report the path
	 * @return the error response
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
	}

	/**
	 * Catch-all. Errors that Spring itself models as HTTP errors (unknown path or static resource such as
	 * {@code /favicon.ico}, unsupported method or media type, ...) keep their own status instead of becoming a 500.
	 *
	 * @param ex      whatever was thrown
	 * @param request the failing request, used to report the path
	 * @return the error response; {@code 500} for anything that is not an HTTP error
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		// Spring's own HTTP exceptions implement its ErrorResponse interface, which carries the right status.
		if (ex instanceof org.springframework.web.ErrorResponse springError) {
			HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
			log.debug("{} for {}: {}", status, request.getRequestURI(), ex.getMessage());
			return build(status, status.getReasonPhrase(), request, null);
		}
		// A genuine bug: log the full stack trace but return a generic message so internals are not leaked.
		log.error("Unexpected error processing {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null);
	}

	/**
	 * Builds the uniform error body.
	 *
	 * @param status  HTTP status to return
	 * @param message human readable explanation
	 * @param request the failing request, used to report the path
	 * @param details optional field level problems, {@code null} when not applicable
	 * @return the response entity carrying the {@link ErrorResponse}
	 */
	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
			List<String> details) {
		ErrorResponse body = ErrorResponse.builder()
				.timestamp(Instant.now())
				.status(status.value())
				.error(status.getReasonPhrase())
				.message(message)
				.path(request.getRequestURI())
				.details(details)
				.build();
		return ResponseEntity.status(status).body(body);
	}
}

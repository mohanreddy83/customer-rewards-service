package com.customer.rewards.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Uniform error body returned by every failing endpoint. Built by
 * {@link com.customer.rewards.exception.RestExceptionHandler}.
 */
@Value
@Builder
@Schema(description = "Error details")
public class ErrorResponse {

	/** Moment the error was produced (UTC). */
	Instant timestamp;

	/** HTTP status code. */
	@Schema(example = "404")
	int status;

	/** HTTP reason phrase matching {@link #status}. */
	@Schema(example = "Not Found")
	String error;

	/** Human readable explanation of what went wrong. */
	@Schema(example = "Customer 99 not found")
	String message;

	/** Request path that failed. */
	@Schema(example = "/api/v1/rewards/99")
	String path;

	/** Field level validation problems ({@code field: message}); {@code null} for other kinds of errors. */
	@Schema(description = "Field level validation problems, if any")
	List<String> details;
}

package com.customer.rewards.exception;

/**
 * Thrown when the requested reporting period is not valid, for example when {@code from} is after {@code to}
 * or the period is longer than allowed. Translated to {@code 400 Bad Request} by {@link RestExceptionHandler}.
 */
public class InvalidDateRangeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message explanation returned to the API client
	 */
	public InvalidDateRangeException(String message) {
		super(message);
	}
}

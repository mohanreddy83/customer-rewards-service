package com.spectrum.customer.rewards.exception;

/**
 * Thrown when a requested resource (for example a customer) does not exist.
 * Translated to {@code 404 Not Found} by {@link RestExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message explanation returned to the API client, e.g. {@code "Customer 99 not found"}
	 */
	public ResourceNotFoundException(String message) {
		super(message);
	}
}

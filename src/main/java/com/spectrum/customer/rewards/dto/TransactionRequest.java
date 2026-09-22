package com.spectrum.customer.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for recording a new purchase.
 *
 * <p>Unlike the response DTOs this class is mutable with a no-args constructor so Jackson can
 * deserialize it; the Bean Validation constraints are enforced by {@code @Valid} in the controller.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A purchase to record")
public class TransactionRequest {

	/** Customer who made the purchase; must exist. */
	@NotNull(message = "customerId is required")
	@Schema(example = "1")
	private Long customerId;

	/** Amount spent in dollars; positive with at most two decimal places (matches the DECIMAL(12,2) column). */
	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.01", message = "amount must be greater than zero")
	@Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
	@Schema(description = "Amount spent in dollars", example = "120.00")
	private BigDecimal amount;

	/** Date of the purchase; future dates are rejected. */
	@NotNull(message = "transactionDate is required")
	@PastOrPresent(message = "transactionDate must not be in the future")
	@Schema(description = "Date of the purchase (ISO-8601)", example = "2026-08-15")
	private LocalDate transactionDate;

	/** Optional free-text description. */
	@Size(max = 200, message = "description must be at most 200 characters")
	@Schema(example = "Electronics store")
	private String description;
}

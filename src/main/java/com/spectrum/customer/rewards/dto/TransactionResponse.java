package com.spectrum.customer.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * A recorded purchase together with the reward points it earned.
 */
@Value
@Builder
@Schema(description = "A recorded purchase and the reward points it earned")
public class TransactionResponse {

	/** Database generated identifier of the transaction. */
	@Schema(example = "42")
	Long transactionId;

	/** Customer who made the purchase. */
	@Schema(example = "1")
	Long customerId;

	/** Amount spent in dollars. */
	@Schema(example = "120.00")
	BigDecimal amount;

	/** Date of the purchase. */
	@Schema(example = "2026-08-15")
	LocalDate transactionDate;

	/** Optional free-text description, {@code null} if none was given. */
	@Schema(example = "Electronics store")
	String description;

	/** Points this single purchase earns under the rewards rules. */
	@Schema(description = "Reward points earned by this purchase", example = "90")
	long rewardPoints;
}

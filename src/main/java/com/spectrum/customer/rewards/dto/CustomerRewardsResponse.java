package com.spectrum.customer.rewards.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Reward points of one customer for a period, per month and in total.
 *
 * <p>Instances are cached by the service layer, which is why the type is immutable ({@code @Value}).</p>
 */
@Value
@Builder
@Schema(description = "Reward points of a customer per month and in total")
public class CustomerRewardsResponse {

	/** Identifier of the customer. */
	@Schema(example = "1")
	Long customerId;

	/** Display name of the customer. */
	@Schema(example = "Alice Johnson")
	String customerName;

	/** First day of the reported period (inclusive). */
	@Schema(description = "First day of the reported period (inclusive)", example = "2026-06-01")
	LocalDate periodStart;

	/** Last day of the reported period (inclusive). */
	@Schema(description = "Last day of the reported period (inclusive)", example = "2026-08-31")
	LocalDate periodEnd;

	/** One entry per calendar month of the period, oldest first, including months without purchases. */
	@Schema(description = "One entry per calendar month of the period, including months without purchases")
	List<MonthlyRewards> monthlyRewards;

	/** Sum of the points of all months in {@link #monthlyRewards}. */
	@Schema(description = "Sum of the monthly points", example = "525")
	long totalPoints;
}

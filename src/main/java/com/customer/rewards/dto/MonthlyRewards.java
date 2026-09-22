package com.customer.rewards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Reward points earned by a customer in a single calendar month.
 */
@Value
@Builder
@Schema(description = "Reward points earned in one calendar month")
public class MonthlyRewards {

	/** Calendar month in {@code yyyy-MM} format. */
	@Schema(description = "Calendar month in yyyy-MM format", example = "2026-06")
	String month;

	/** Number of purchases the customer made in the month (0 when none). */
	@Schema(description = "Number of purchases made in the month", example = "2")
	int transactionCount;

	/** Sum of the points of every purchase in the month. */
	@Schema(description = "Reward points earned in the month", example = "115")
	long points;
}

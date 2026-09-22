package com.spectrum.customer.rewards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Applies the rewards program rules to a purchase amount.
 *
 * <ul>
 *   <li>2 points for every dollar spent over $100 in a transaction</li>
 *   <li>1 point for every dollar spent between $50 and $100 in a transaction</li>
 * </ul>
 *
 * <p>Example: a $120 purchase earns {@code 2 x $20 + 1 x $50 = 90} points.
 * Only whole dollars count; cents are truncated (a $50.99 purchase earns 0 points,
 * a $100.99 purchase earns 50 points).</p>
 */
@Component
public class RewardPointsCalculator {

	/** Spend above this amount (per transaction) starts earning points. */
	static final long LOWER_THRESHOLD = 50;

	/** Spend above this amount (per transaction) earns double points. */
	static final long UPPER_THRESHOLD = 100;

	/**
	 * Calculates the points earned by a single transaction.
	 *
	 * @param amount the transaction amount in dollars; {@code null} or non-positive amounts earn nothing
	 * @return the reward points earned, never negative
	 */
	public long calculatePoints(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			return 0;
		}
		// Whole dollars only: DOWN truncates the cents (120.99 -> 120).
		long dollars = amount.setScale(0, RoundingMode.DOWN).longValue();
		// Dollars above $100 earn 2 points each, e.g. $120 -> 20 dollars over -> 40 points.
		long doublePoints = Math.max(0, dollars - UPPER_THRESHOLD) * 2;
		// Dollars in the $50-$100 band earn 1 point each; capped at $100 because the excess is already counted above.
		long singlePoints = Math.max(0, Math.min(dollars, UPPER_THRESHOLD) - LOWER_THRESHOLD);
		return doublePoints + singlePoints;
	}
}

package com.customer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for the rewards rules: 1 point per dollar between $50 and $100, 2 points per dollar over $100,
 * whole dollars only. The table below concentrates on the boundaries where off-by-one mistakes hide.
 */
class RewardPointsCalculatorTest {

	private final RewardPointsCalculator calculator = new RewardPointsCalculator();

	// Columns: amount, expected points. Comments show the arithmetic for the interesting rows.
	@ParameterizedTest(name = "${0} earns {1} points")
	@CsvSource({
			"0.00, 0",
			"20.00, 0",
			"49.99, 0",
			"50.00, 0", // exactly $50 is not "between", nothing earned
			"50.99, 0", // cents are truncated -> $50
			"51.00, 1", // first dollar in the 50-100 band
			"75.00, 25",
			"99.99, 49",
			"100.00, 50", // 1 x $50, nothing over $100 yet
			"100.99, 50", // truncated to $100
			"101.00, 52", // 2 x $1 + 1 x $50
			"120.00, 90", // the example from the requirements: 2 x $20 + 1 x $50
			"120.75, 90",
			"250.00, 350", // 2 x $150 + 1 x $50
			"500.00, 850"
	})
	void calculatesPointsForAmount(String amount, long expectedPoints) {
		assertThat(calculator.calculatePoints(new BigDecimal(amount))).isEqualTo(expectedPoints);
	}

	@Test
	void nullAmountEarnsNothing() {
		assertThat(calculator.calculatePoints(null)).isZero();
	}

	@Test
	void negativeAmountEarnsNothing() {
		assertThat(calculator.calculatePoints(new BigDecimal("-150.00"))).isZero();
	}
}

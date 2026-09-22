package com.spectrum.customer.rewards.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spectrum.customer.rewards.dto.CustomerRewardsResponse;
import com.spectrum.customer.rewards.dto.ErrorResponse;
import com.spectrum.customer.rewards.service.RewardsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST endpoints exposing reward points per customer, per month and in total.
 *
 * <p>The controller only parses the request and delegates; the period rules and calculation live in
 * {@link RewardsService}. Errors are converted to HTTP responses by
 * {@link com.spectrum.customer.rewards.exception.RestExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards", description = "Reward points per customer, per month and in total")
public class RewardsController {

	private final RewardsService rewardsService;

	// Note: @DateTimeFormat(iso = DATE) on the parameters below makes Spring parse "yyyy-MM-dd" query values;
	// an unparsable value is reported as 400 by RestExceptionHandler.

	/**
	 * Returns the reward points of every customer for a period.
	 *
	 * @param from first day of the period (inclusive), optional
	 * @param to   last day of the period (inclusive), optional
	 * @return one entry per customer with monthly and total points
	 */
	@Operation(summary = "Rewards of all customers",
			description = "Returns monthly and total reward points for every customer. Without from/to the three "
					+ "calendar months ending with the month of the latest transaction are reported.")
	@ApiResponse(responseCode = "200", description = "Rewards calculated")
	@ApiResponse(responseCode = "400", description = "Invalid period",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public List<CustomerRewardsResponse> getAllRewards(
			@Parameter(description = "First day of the period, ISO date", example = "2026-06-01")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Last day of the period, ISO date", example = "2026-08-31")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return rewardsService.getAllCustomerRewards(from, to);
	}

	/**
	 * Returns the reward points of a single customer for a period.
	 *
	 * @param customerId the customer identifier
	 * @param from       first day of the period (inclusive), optional
	 * @param to         last day of the period (inclusive), optional
	 * @return the customer's monthly and total points
	 */
	@Operation(summary = "Rewards of one customer",
			description = "Returns monthly and total reward points for a single customer.")
	@ApiResponse(responseCode = "200", description = "Rewards calculated")
	@ApiResponse(responseCode = "400", description = "Invalid period",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Customer not found",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{customerId}")
	public CustomerRewardsResponse getCustomerRewards(
			@Parameter(description = "Customer identifier", example = "1") @PathVariable Long customerId,
			@Parameter(description = "First day of the period, ISO date", example = "2026-06-01")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Last day of the period, ISO date", example = "2026-08-31")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return rewardsService.getCustomerRewards(customerId, from, to);
	}
}

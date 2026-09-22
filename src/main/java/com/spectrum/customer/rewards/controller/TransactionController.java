package com.spectrum.customer.rewards.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.spectrum.customer.rewards.dto.ErrorResponse;
import com.spectrum.customer.rewards.dto.TransactionRequest;
import com.spectrum.customer.rewards.dto.TransactionResponse;
import com.spectrum.customer.rewards.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST endpoints to record and list purchase transactions.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Record and list purchases")
public class TransactionController {

	private final TransactionService transactionService;

	/**
	 * Records a purchase for a customer.
	 *
	 * @param request the validated purchase details
	 * @return {@code 201 Created} with the stored purchase and the points it earned; the {@code Location}
	 *         header points at the customer's transaction list
	 */
	@Operation(summary = "Record a purchase",
			description = "Stores a purchase and returns it with the reward points it earned. "
					+ "Cached reward calculations are refreshed.")
	@ApiResponse(responseCode = "201", description = "Purchase recorded")
	@ApiResponse(responseCode = "400", description = "Validation failed",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Customer not found",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
		TransactionResponse created = transactionService.createTransaction(request);
		// There is no "get one transaction" endpoint, so Location points at the list filtered by the customer.
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.queryParam("customerId", created.getCustomerId())
				.build()
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	/**
	 * Lists the purchases of a customer, oldest first.
	 *
	 * @param customerId the customer identifier
	 * @return the customer's purchases, each with the points it earned
	 */
	@Operation(summary = "List purchases of a customer")
	@ApiResponse(responseCode = "200", description = "Purchases listed")
	@ApiResponse(responseCode = "404", description = "Customer not found",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public List<TransactionResponse> getTransactions(
			@Parameter(description = "Customer identifier", example = "1", required = true)
			@RequestParam Long customerId) {
		return transactionService.getTransactionsForCustomer(customerId);
	}
}

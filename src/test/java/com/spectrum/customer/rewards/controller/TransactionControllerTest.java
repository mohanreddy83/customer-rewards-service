package com.spectrum.customer.rewards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spectrum.customer.rewards.dto.TransactionResponse;
import com.spectrum.customer.rewards.exception.ResourceNotFoundException;
import com.spectrum.customer.rewards.service.TransactionService;

/**
 * Web layer tests for {@link TransactionController}: request validation, status codes and the
 * {@code Location} header. {@link TransactionService} is mocked.
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TransactionService transactionService;

	/** Test helper: a fixed response used as the mocked service result. */
	private static TransactionResponse sample() {
		return TransactionResponse.builder()
				.transactionId(42L)
				.customerId(1L)
				.amount(new BigDecimal("120.00"))
				.transactionDate(LocalDate.of(2026, 8, 15))
				.description("Shoes")
				.rewardPoints(90)
				.build();
	}

	@Test
	void createsTransaction() throws Exception {
		when(transactionService.createTransaction(any())).thenReturn(sample());

		mockMvc.perform(post("/api/v1/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 1, "amount": 120.00, "transactionDate": "2026-08-15", "description": "Shoes"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/transactions?customerId=1"))
				.andExpect(jsonPath("$.transactionId").value(42))
				.andExpect(jsonPath("$.rewardPoints").value(90));
	}

	// Three violations at once: missing customerId, negative amount, future date. The service must not be called.
	@Test
	void rejectsInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/v1/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": -5, "transactionDate": "2999-01-01"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.details.length()").value(3));
		verifyNoInteractions(transactionService);
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content("{oops"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void unknownCustomerYieldsNotFound() throws Exception {
		when(transactionService.createTransaction(any()))
				.thenThrow(new ResourceNotFoundException("Customer 99 not found"));

		mockMvc.perform(post("/api/v1/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 99, "amount": 10.00, "transactionDate": "2026-08-15"}
						"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void listsTransactionsOfCustomer() throws Exception {
		when(transactionService.getTransactionsForCustomer(1L)).thenReturn(List.of(sample()));

		mockMvc.perform(get("/api/v1/transactions").param("customerId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].transactionId").value(42));
	}

	@Test
	void listingRequiresCustomerId() throws Exception {
		mockMvc.perform(get("/api/v1/transactions"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Missing required parameter 'customerId'"));
	}
}

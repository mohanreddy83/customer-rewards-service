package com.customer.rewards.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.customer.rewards.dto.CustomerRewardsResponse;
import com.customer.rewards.dto.MonthlyRewards;
import com.customer.rewards.exception.InvalidDateRangeException;
import com.customer.rewards.exception.ResourceNotFoundException;
import com.customer.rewards.service.RewardsService;

/**
 * Web layer tests for {@link RewardsController}: routing, parameter parsing, JSON shape and error mapping.
 * Only the MVC slice is loaded and {@link RewardsService} is mocked, so no database is involved.
 */
@WebMvcTest(RewardsController.class)
class RewardsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RewardsService rewardsService;

	/** Test helper: a small, fixed response used as the mocked service result. */
	private static CustomerRewardsResponse sample() {
		return CustomerRewardsResponse.builder()
				.customerId(1L)
				.customerName("Alice")
				.periodStart(LocalDate.of(2026, 6, 1))
				.periodEnd(LocalDate.of(2026, 6, 30))
				.monthlyRewards(List.of(MonthlyRewards.builder().month("2026-06").transactionCount(1).points(90).build()))
				.totalPoints(90)
				.build();
	}

	@Test
	void returnsRewardsOfOneCustomer() throws Exception {
		when(rewardsService.getCustomerRewards(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
				.thenReturn(sample());

		mockMvc.perform(get("/api/v1/rewards/1").param("from", "2026-06-01").param("to", "2026-06-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerName").value("Alice"))
				.andExpect(jsonPath("$.periodStart").value("2026-06-01"))
				.andExpect(jsonPath("$.monthlyRewards[0].month").value("2026-06"))
				.andExpect(jsonPath("$.monthlyRewards[0].points").value(90))
				.andExpect(jsonPath("$.totalPoints").value(90));
	}

	@Test
	void returnsRewardsOfAllCustomers() throws Exception {
		when(rewardsService.getAllCustomerRewards(null, null)).thenReturn(List.of(sample()));

		mockMvc.perform(get("/api/v1/rewards"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].totalPoints").value(90));
	}

	@Test
	void unknownCustomerYieldsNotFound() throws Exception {
		when(rewardsService.getCustomerRewards(99L, null, null))
				.thenThrow(new ResourceNotFoundException("Customer 99 not found"));

		mockMvc.perform(get("/api/v1/rewards/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Customer 99 not found"))
				.andExpect(jsonPath("$.path").value("/api/v1/rewards/99"));
	}

	@Test
	void invalidPeriodYieldsBadRequest() throws Exception {
		when(rewardsService.getAllCustomerRewards(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1)))
				.thenThrow(new InvalidDateRangeException("'from' must not be after 'to'"));

		mockMvc.perform(get("/api/v1/rewards").param("from", "2026-08-01").param("to", "2026-07-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("'from' must not be after 'to'"));
	}

	@Test
	void malformedDateYieldsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/rewards").param("from", "not-a-date"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value 'not-a-date' for parameter 'from'"));
	}

	// Regression test: browsers request /favicon.ico, which used to surface as a 500 with a stack trace.
	@Test
	void unknownPathYieldsNotFoundInsteadOfServerError() throws Exception {
		mockMvc.perform(get("/favicon.ico"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void unsupportedMethodYieldsMethodNotAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/rewards/1")).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void nonNumericCustomerIdYieldsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/rewards/abc")).andExpect(status().isBadRequest());
	}
}

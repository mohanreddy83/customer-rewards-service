package com.customer.rewards.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.customer.rewards.dto.CustomerResponse;
import com.customer.rewards.service.CustomerService;

/**
 * Web layer test for {@link CustomerController} with a mocked {@link CustomerService}.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CustomerService customerService;

	@Test
	void listsCustomers() throws Exception {
		when(customerService.getAllCustomers())
				.thenReturn(List.of(CustomerResponse.builder().customerId(1L).name("Alice").build()));

		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].customerId").value(1))
				.andExpect(jsonPath("$[0].name").value("Alice"));
	}
}

package com.customer.rewards.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.customer.rewards.dto.CustomerResponse;
import com.customer.rewards.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST endpoint listing the customers of the rewards program.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customers of the rewards program")
public class CustomerController {

	private final CustomerService customerService;

	/**
	 * Lists the customers of the rewards program.
	 *
	 * @return all customers, ordered by customer id
	 */
	@Operation(summary = "List all customers")
	@GetMapping
	public List<CustomerResponse> getCustomers() {
		return customerService.getAllCustomers();
	}
}

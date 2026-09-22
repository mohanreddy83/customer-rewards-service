package com.spectrum.customer.rewards.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spectrum.customer.rewards.dao.CustomerDao;
import com.spectrum.customer.rewards.dto.CustomerResponse;

import lombok.RequiredArgsConstructor;

/**
 * Read access to customers of the rewards program.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerDao customerDao;

	/**
	 * Lists every customer.
	 *
	 * @return all customers, ordered by customer id
	 */
	public List<CustomerResponse> getAllCustomers() {
		// Map the internal model to the API DTO so the persistence model can evolve independently.
		return customerDao.findAll().stream()
				.map(c -> CustomerResponse.builder().customerId(c.getCustomerId()).name(c.getName()).build())
				.toList();
	}
}

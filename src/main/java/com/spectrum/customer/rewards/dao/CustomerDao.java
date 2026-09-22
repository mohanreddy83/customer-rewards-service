package com.spectrum.customer.rewards.dao;

import java.util.List;
import java.util.Optional;

import com.spectrum.customer.rewards.model.Customer;

/**
 * Data access contract for {@link Customer} records.
 */
public interface CustomerDao {

	/**
	 * @return every customer, ordered by customer id
	 */
	List<Customer> findAll();

	/**
	 * @param customerId the customer identifier
	 * @return the customer, or empty if none exists with that id
	 */
	Optional<Customer> findById(Long customerId);
}

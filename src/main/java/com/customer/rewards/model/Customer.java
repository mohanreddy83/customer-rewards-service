package com.customer.rewards.model;

import lombok.Builder;
import lombok.Value;

/**
 * A customer enrolled in the rewards program. Maps to a row of the {@code customer} table.
 */
@Value
@Builder
public class Customer {

	/** Unique customer identifier (primary key). */
	Long customerId;

	/** Display name of the customer. */
	String name;
}

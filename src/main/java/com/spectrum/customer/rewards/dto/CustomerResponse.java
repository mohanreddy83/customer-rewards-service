package com.spectrum.customer.rewards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Public representation of a customer.
 */
@Value
@Builder
@Schema(description = "A customer enrolled in the rewards program")
public class CustomerResponse {

	/** Identifier of the customer. */
	@Schema(example = "1")
	Long customerId;

	/** Display name of the customer. */
	@Schema(example = "Alice Johnson")
	String name;
}

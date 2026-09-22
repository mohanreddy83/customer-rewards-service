package com.customer.rewards.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

/**
 * A single recorded purchase made by a customer. Maps to a row of the {@code purchase_transaction} table.
 */
@Value
@Builder
public class PurchaseTransaction {

	/** Unique transaction identifier; {@code null} until the transaction has been persisted. */
	Long transactionId;

	/** Identifier of the customer who made the purchase (foreign key to {@code customer}). */
	Long customerId;

	/** Amount spent, in dollars, with two decimal places. */
	BigDecimal amount;

	/** Date the purchase was made; decides which calendar month the points belong to. */
	LocalDate transactionDate;

	/** Optional free-text description of the purchase. */
	String description;
}

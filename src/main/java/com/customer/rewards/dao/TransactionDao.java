package com.customer.rewards.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.customer.rewards.model.PurchaseTransaction;

/**
 * Data access contract for {@link PurchaseTransaction} records.
 */
public interface TransactionDao {

	/**
	 * Persists a new transaction.
	 *
	 * @param transaction the transaction to store; its id is ignored
	 * @return the stored transaction including its generated id
	 */
	PurchaseTransaction save(PurchaseTransaction transaction);

	/**
	 * @param customerId the customer identifier
	 * @return all transactions of the customer, oldest first
	 */
	List<PurchaseTransaction> findByCustomerId(Long customerId);

	/**
	 * @param from first day (inclusive)
	 * @param to   last day (inclusive)
	 * @return all transactions of all customers in the date range, oldest first
	 */
	List<PurchaseTransaction> findByDateRange(LocalDate from, LocalDate to);

	/**
	 * @param customerId the customer identifier
	 * @param from       first day (inclusive)
	 * @param to         last day (inclusive)
	 * @return the customer's transactions in the date range, oldest first
	 */
	List<PurchaseTransaction> findByCustomerIdAndDateRange(Long customerId, LocalDate from, LocalDate to);

	/**
	 * @return the date of the most recent transaction, or empty if there are none
	 */
	Optional<LocalDate> findLatestTransactionDate();
}

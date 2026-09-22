package com.spectrum.customer.rewards.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

import com.spectrum.customer.rewards.dao.impl.JdbcCustomerDao;
import com.spectrum.customer.rewards.dao.impl.JdbcTransactionDao;
import com.spectrum.customer.rewards.model.Customer;
import com.spectrum.customer.rewards.model.PurchaseTransaction;

/**
 * Runs the DAOs against the embedded HSQLDB seeded by {@code schema.sql} and {@code data.sql}.
 * Each test runs in a transaction that is rolled back.
 */
@JdbcTest
@Import({ JdbcCustomerDao.class, JdbcTransactionDao.class })
class JdbcDaoTest {

	@Autowired
	private CustomerDao customerDao;

	@Autowired
	private TransactionDao transactionDao;

	@Test
	void findsAllCustomersOrderedById() {
		assertThat(customerDao.findAll()).extracting(Customer::getCustomerId).containsExactly(1L, 2L, 3L, 4L, 5L);
	}

	@Test
	void findsCustomerById() {
		assertThat(customerDao.findById(3L)).get().extracting(Customer::getName).isEqualTo("Carol Williams");
		assertThat(customerDao.findById(99L)).isEmpty();
	}

	@Test
	void findsTransactionsOfCustomerOldestFirst() {
		List<PurchaseTransaction> transactions = transactionDao.findByCustomerId(1L);

		assertThat(transactions).hasSize(7);
		assertThat(transactions).extracting(PurchaseTransaction::getTransactionDate).isSorted();
		assertThat(transactions.get(0).getAmount()).isEqualByComparingTo("300.00");
	}

	@Test
	void findsTransactionsInDateRangeInclusive() {
		List<PurchaseTransaction> transactions = transactionDao.findByCustomerIdAndDateRange(1L,
				LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 18));

		assertThat(transactions).extracting(PurchaseTransaction::getTransactionDate)
				.containsExactly(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 18));
	}

	@Test
	void findsTransactionsOfAllCustomersInDateRange() {
		List<PurchaseTransaction> transactions = transactionDao.findByDateRange(LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 30));

		assertThat(transactions).extracting(PurchaseTransaction::getCustomerId).containsOnly(1L, 2L, 3L);
		assertThat(transactions).hasSize(5);
	}

	@Test
	void findsLatestTransactionDate() {
		assertThat(transactionDao.findLatestTransactionDate()).contains(LocalDate.of(2026, 8, 30));
	}

	@Test
	void savesTransactionAndGeneratesId() {
		PurchaseTransaction saved = transactionDao.save(PurchaseTransaction.builder()
				.customerId(5L)
				.amount(new BigDecimal("80.00"))
				.transactionDate(LocalDate.of(2026, 9, 1))
				.description("Test purchase")
				.build());

		assertThat(saved.getTransactionId()).isNotNull();
		assertThat(transactionDao.findByCustomerId(5L)).extracting(PurchaseTransaction::getTransactionId)
				.contains(saved.getTransactionId());
		assertThat(transactionDao.findLatestTransactionDate()).contains(LocalDate.of(2026, 9, 1));
	}
}

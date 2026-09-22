package com.spectrum.customer.rewards.dao.impl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.spectrum.customer.rewards.dao.TransactionDao;
import com.spectrum.customer.rewards.model.PurchaseTransaction;

import lombok.RequiredArgsConstructor;

/**
 * {@link TransactionDao} implementation backed by {@link NamedParameterJdbcTemplate}.
 *
 * <p>Named parameters ({@code :customerId}) are used instead of positional {@code ?} markers because the
 * queries have several parameters, and all values are bound (never concatenated) to avoid SQL injection.</p>
 */
@Repository
@RequiredArgsConstructor
public class JdbcTransactionDao implements TransactionDao {

	/** Shared column list and table; each finder appends its own WHERE / ORDER BY clause. */
	private static final String SELECT = """
			SELECT transaction_id, customer_id, amount, transaction_date, description
			FROM purchase_transaction
			""";

	/** Converts one result set row into a {@link PurchaseTransaction}. */
	private static final RowMapper<PurchaseTransaction> ROW_MAPPER = (rs, rowNum) -> PurchaseTransaction.builder()
			.transactionId(rs.getLong("transaction_id"))
			.customerId(rs.getLong("customer_id"))
			.amount(rs.getBigDecimal("amount"))
			.transactionDate(rs.getObject("transaction_date", LocalDate.class))
			.description(rs.getString("description"))
			.build();

	private final NamedParameterJdbcTemplate jdbcTemplate;

	/** {@inheritDoc} */
	@Override
	public PurchaseTransaction save(PurchaseTransaction transaction) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("customerId", transaction.getCustomerId())
				.addValue("amount", transaction.getAmount())
				.addValue("transactionDate", Date.valueOf(transaction.getTransactionDate()))
				.addValue("description", transaction.getDescription());

		// transaction_id is an IDENTITY column: ask the driver to hand back the generated value.
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update("""
				INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description)
				VALUES (:customerId, :amount, :transactionDate, :description)
				""", params, keyHolder, new String[] { "transaction_id" });

		// The model is immutable, so return a copy that carries the generated id.
		return PurchaseTransaction.builder()
				.transactionId(keyHolder.getKey().longValue())
				.customerId(transaction.getCustomerId())
				.amount(transaction.getAmount())
				.transactionDate(transaction.getTransactionDate())
				.description(transaction.getDescription())
				.build();
	}

	/** {@inheritDoc} */
	@Override
	public List<PurchaseTransaction> findByCustomerId(Long customerId) {
		// transaction_id is a tie breaker so the order of same-day purchases is deterministic.
		return jdbcTemplate.query(SELECT + " WHERE customer_id = :customerId ORDER BY transaction_date, transaction_id",
				new MapSqlParameterSource("customerId", customerId), ROW_MAPPER);
	}

	/** {@inheritDoc} */
	@Override
	public List<PurchaseTransaction> findByDateRange(LocalDate from, LocalDate to) {
		// BETWEEN is inclusive on both ends, matching the documented contract.
		return jdbcTemplate.query(
				SELECT + " WHERE transaction_date BETWEEN :from AND :to ORDER BY transaction_date, transaction_id",
				dateRange(from, to), ROW_MAPPER);
	}

	/** {@inheritDoc} */
	@Override
	public List<PurchaseTransaction> findByCustomerIdAndDateRange(Long customerId, LocalDate from, LocalDate to) {
		// The (customer_id, transaction_date) index defined in schema.sql serves this query.
		return jdbcTemplate.query(SELECT + """
				 WHERE customer_id = :customerId AND transaction_date BETWEEN :from AND :to
				 ORDER BY transaction_date, transaction_id
				""", dateRange(from, to).addValue("customerId", customerId), ROW_MAPPER);
	}

	/** {@inheritDoc} */
	@Override
	public Optional<LocalDate> findLatestTransactionDate() {
		// MAX() over an empty table yields a single row containing NULL, hence the Optional wrapping.
		Date latest = jdbcTemplate.getJdbcOperations()
				.queryForObject("SELECT MAX(transaction_date) FROM purchase_transaction", Date.class);
		return Optional.ofNullable(latest).map(Date::toLocalDate);
	}

	/**
	 * Creates the {@code :from} / {@code :to} parameters shared by the date range queries.
	 *
	 * @param from first day (inclusive)
	 * @param to   last day (inclusive)
	 * @return a parameter source the caller may extend with further parameters
	 */
	private static MapSqlParameterSource dateRange(LocalDate from, LocalDate to) {
		return new MapSqlParameterSource()
				.addValue("from", Date.valueOf(from))
				.addValue("to", Date.valueOf(to));
	}
}

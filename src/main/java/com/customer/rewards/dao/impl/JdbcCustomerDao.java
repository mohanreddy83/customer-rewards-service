package com.customer.rewards.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.customer.rewards.dao.CustomerDao;
import com.customer.rewards.model.Customer;

import lombok.RequiredArgsConstructor;

/**
 * {@link CustomerDao} implementation backed by {@link JdbcTemplate}.
 *
 * <p>Marked {@link Repository} so Spring registers it as a bean and translates JDBC exceptions into
 * Spring's {@code DataAccessException} hierarchy.</p>
 */
@Repository
@RequiredArgsConstructor
public class JdbcCustomerDao implements CustomerDao {

	/** Converts one result set row into a {@link Customer}. Stateless, so a shared constant is safe. */
	private static final RowMapper<Customer> ROW_MAPPER = (rs, rowNum) -> Customer.builder()
			.customerId(rs.getLong("customer_id"))
			.name(rs.getString("name"))
			.build();

	private final JdbcTemplate jdbcTemplate;

	/** {@inheritDoc} */
	@Override
	public List<Customer> findAll() {
		return jdbcTemplate.query("SELECT customer_id, name FROM customer ORDER BY customer_id", ROW_MAPPER);
	}

	/** {@inheritDoc} */
	@Override
	public Optional<Customer> findById(Long customerId) {
		// The query has at most one row (primary key lookup); stream().findFirst() turns "no row" into Optional.empty().
		return jdbcTemplate
				.query("SELECT customer_id, name FROM customer WHERE customer_id = ?", ROW_MAPPER, customerId)
				.stream()
				.findFirst();
	}
}

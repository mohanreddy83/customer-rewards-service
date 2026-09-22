package com.customer.rewards.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.customer.rewards.config.CacheConfig;
import com.customer.rewards.dao.CustomerDao;
import com.customer.rewards.dao.TransactionDao;
import com.customer.rewards.dto.CustomerRewardsResponse;
import com.customer.rewards.dto.MonthlyRewards;
import com.customer.rewards.exception.InvalidDateRangeException;
import com.customer.rewards.exception.ResourceNotFoundException;
import com.customer.rewards.model.Customer;
import com.customer.rewards.model.PurchaseTransaction;

import lombok.RequiredArgsConstructor;

/**
 * Calculates reward points per customer, per calendar month and in total.
 *
 * <p>Results are cached; the caches are evicted by {@link TransactionService} whenever a new
 * transaction is recorded.</p>
 *
 * <h2>Reporting period</h2>
 * <ul>
 *   <li>{@code from} and {@code to} both given: that exact period.</li>
 *   <li>Only {@code to} given: the three calendar months ending with the month of {@code to}.</li>
 *   <li>Only {@code from} given: the three calendar months starting with the month of {@code from}.</li>
 *   <li>Neither given: the three calendar months ending with the month of the most recent
 *       transaction (or the current month when there are no transactions).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RewardsService {

	private static final Logger log = LoggerFactory.getLogger(RewardsService.class);

	/** Number of calendar months reported when the period is not fully specified. */
	static final int DEFAULT_MONTHS = 3;

	/** Longest period, in days, that may be requested. */
	static final int MAX_PERIOD_DAYS = 366;

	private final CustomerDao customerDao;
	private final TransactionDao transactionDao;
	private final RewardPointsCalculator calculator;

	/**
	 * Returns the reward points of one customer.
	 *
	 * @param customerId the customer identifier
	 * @param from       first day of the period (inclusive), may be {@code null}
	 * @param to         last day of the period (inclusive), may be {@code null}
	 * @return the customer's points per month and in total
	 * @throws ResourceNotFoundException if the customer does not exist
	 * @throws InvalidDateRangeException if the period is invalid
	 */
	@Cacheable(cacheNames = CacheConfig.CUSTOMER_REWARDS, key = "#customerId + '|' + #from + '|' + #to")
	public CustomerRewardsResponse getCustomerRewards(Long customerId, LocalDate from, LocalDate to) {
		// Fail fast with a 404 before doing any transaction lookups.
		Customer customer = customerDao.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer " + customerId + " not found"));
		LocalDate[] period = resolvePeriod(from, to);
		log.debug("Calculating rewards for customer {} from {} to {}", customerId, period[0], period[1]);

		List<PurchaseTransaction> transactions = transactionDao.findByCustomerIdAndDateRange(customerId, period[0],
				period[1]);
		return buildResponse(customer, transactions, period[0], period[1]);
	}

	/**
	 * Returns the reward points of every customer.
	 *
	 * @param from first day of the period (inclusive), may be {@code null}
	 * @param to   last day of the period (inclusive), may be {@code null}
	 * @return one entry per customer, ordered by customer id
	 * @throws InvalidDateRangeException if the period is invalid
	 */
	@Cacheable(cacheNames = CacheConfig.ALL_REWARDS, key = "#from + '|' + #to")
	public List<CustomerRewardsResponse> getAllCustomerRewards(LocalDate from, LocalDate to) {
		LocalDate[] period = resolvePeriod(from, to);
		log.debug("Calculating rewards for all customers from {} to {}", period[0], period[1]);

		// One query for everybody, then split per customer in memory (avoids one query per customer).
		Map<Long, List<PurchaseTransaction>> byCustomer = transactionDao.findByDateRange(period[0], period[1])
				.stream()
				.collect(Collectors.groupingBy(PurchaseTransaction::getCustomerId));
		// Start from the customer list, not the transactions, so customers without purchases still appear (0 points).
		return customerDao.findAll().stream()
				.map(customer -> buildResponse(customer,
						byCustomer.getOrDefault(customer.getCustomerId(), Collections.emptyList()), period[0],
						period[1]))
				.toList();
	}

	/**
	 * Groups a customer's transactions by calendar month and applies the rewards rules to each.
	 *
	 * @param customer     the customer the transactions belong to
	 * @param transactions the customer's transactions, already restricted to {@code from..to}
	 * @param from         first day of the period (inclusive)
	 * @param to           last day of the period (inclusive)
	 * @return the customer's points per month (every month of the period) and in total
	 */
	private CustomerRewardsResponse buildResponse(Customer customer, List<PurchaseTransaction> transactions,
			LocalDate from, LocalDate to) {
		Map<YearMonth, List<PurchaseTransaction>> byMonth = transactions.stream()
				.collect(Collectors.groupingBy(t -> YearMonth.from(t.getTransactionDate())));

		List<MonthlyRewards> monthly = new ArrayList<>();
		long total = 0;
		// Walk every month of the period (not just months that have data) so quiet months show up as 0.
		for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
			List<PurchaseTransaction> inMonth = byMonth.getOrDefault(month, Collections.emptyList());
			// Points are calculated per transaction and then summed; the rules are not applied to the monthly total.
			long points = inMonth.stream().mapToLong(t -> calculator.calculatePoints(t.getAmount())).sum();
			monthly.add(MonthlyRewards.builder()
					.month(month.toString())
					.transactionCount(inMonth.size())
					.points(points)
					.build());
			total += points;
		}
		return CustomerRewardsResponse.builder()
				.customerId(customer.getCustomerId())
				.customerName(customer.getName())
				.periodStart(from)
				.periodEnd(to)
				.monthlyRewards(monthly)
				.totalPoints(total)
				.build();
	}

	/**
	 * Applies the period rules described in the class documentation.
	 *
	 * @return a two element array: first day (inclusive) and last day (inclusive)
	 */
	private LocalDate[] resolvePeriod(LocalDate from, LocalDate to) {
		LocalDate start = from;
		LocalDate end = to;
		if (start == null && end == null) {
			// Anchor on the latest data rather than "today" so the report is meaningful for historical data sets.
			LocalDate anchor = transactionDao.findLatestTransactionDate().orElseGet(LocalDate::now);
			end = YearMonth.from(anchor).atEndOfMonth();
			start = YearMonth.from(anchor).minusMonths(DEFAULT_MONTHS - 1L).atDay(1);
		}
		else if (start == null) {
			start = YearMonth.from(end).minusMonths(DEFAULT_MONTHS - 1L).atDay(1);
		}
		else if (end == null) {
			end = YearMonth.from(start).plusMonths(DEFAULT_MONTHS - 1L).atEndOfMonth();
		}

		// Whole months are used for defaults: first day of the earliest month to last day of the latest month.
		if (start.isAfter(end)) {
			throw new InvalidDateRangeException("'from' (" + start + ") must not be after 'to' (" + end + ")");
		}
		// Guard against accidental huge ranges (each month becomes a response entry).
		if (start.plusDays(MAX_PERIOD_DAYS).isBefore(end)) {
			throw new InvalidDateRangeException("The period must not exceed " + MAX_PERIOD_DAYS + " days");
		}
		return new LocalDate[] { start, end };
	}
}

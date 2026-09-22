package com.spectrum.customer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spectrum.customer.rewards.dao.CustomerDao;
import com.spectrum.customer.rewards.dao.TransactionDao;
import com.spectrum.customer.rewards.dto.CustomerRewardsResponse;
import com.spectrum.customer.rewards.dto.MonthlyRewards;
import com.spectrum.customer.rewards.exception.InvalidDateRangeException;
import com.spectrum.customer.rewards.exception.ResourceNotFoundException;
import com.spectrum.customer.rewards.model.Customer;
import com.spectrum.customer.rewards.model.PurchaseTransaction;

/**
 * Unit tests for {@link RewardsService}. The DAOs are mocked; the real {@link RewardPointsCalculator} is used so
 * the tests verify grouping, period resolution and totals end to end within the service.
 * Caching is not active here (no Spring context); see the integration test for that.
 */
@ExtendWith(MockitoExtension.class)
class RewardsServiceTest {

	private static final Customer ALICE = Customer.builder().customerId(1L).name("Alice").build();
	private static final Customer BOB = Customer.builder().customerId(2L).name("Bob").build();

	@Mock
	private CustomerDao customerDao;

	@Mock
	private TransactionDao transactionDao;

	private RewardsService service;

	@BeforeEach
	void setUp() {
		service = new RewardsService(customerDao, transactionDao, new RewardPointsCalculator());
	}

	@Test
	void calculatesMonthlyAndTotalPointsForExplicitPeriod() {
		LocalDate from = LocalDate.of(2026, 6, 1);
		LocalDate to = LocalDate.of(2026, 8, 31);
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		when(transactionDao.findByCustomerIdAndDateRange(1L, from, to)).thenReturn(List.of(
				tx(1L, "120.00", LocalDate.of(2026, 6, 5)), // 90 points
				tx(1L, "75.00", LocalDate.of(2026, 6, 18)), // 25 points -> June = 115
				tx(1L, "200.00", LocalDate.of(2026, 8, 9)))); // 250 points; July has no purchases

		CustomerRewardsResponse response = service.getCustomerRewards(1L, from, to);

		assertThat(response.getCustomerName()).isEqualTo("Alice");
		assertThat(response.getPeriodStart()).isEqualTo(from);
		assertThat(response.getPeriodEnd()).isEqualTo(to);
		assertThat(response.getMonthlyRewards())
				.extracting(MonthlyRewards::getMonth, MonthlyRewards::getTransactionCount, MonthlyRewards::getPoints)
				.containsExactly(
						tuple("2026-06", 2, 115L),
						tuple("2026-07", 0, 0L), // months without purchases are still reported
						tuple("2026-08", 1, 250L));
		assertThat(response.getTotalPoints()).isEqualTo(365); // 115 + 0 + 250
	}

	@Test
	void defaultPeriodIsThreeMonthsEndingWithLatestTransactionMonth() {
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		when(transactionDao.findLatestTransactionDate()).thenReturn(Optional.of(LocalDate.of(2026, 8, 25)));
		when(transactionDao.findByCustomerIdAndDateRange(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31)))
				.thenReturn(List.of());

		CustomerRewardsResponse response = service.getCustomerRewards(1L, null, null);

		assertThat(response.getPeriodStart()).isEqualTo(LocalDate.of(2026, 6, 1));
		assertThat(response.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 8, 31));
		assertThat(response.getMonthlyRewards()).extracting(MonthlyRewards::getMonth)
				.containsExactly("2026-06", "2026-07", "2026-08");
		assertThat(response.getTotalPoints()).isZero();
	}

	// Guards the month arithmetic: the window must roll back into the previous year.
	@Test
	void defaultPeriodCrossesYearBoundary() {
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		when(transactionDao.findLatestTransactionDate()).thenReturn(Optional.of(LocalDate.of(2026, 1, 10)));
		when(transactionDao.findByCustomerIdAndDateRange(1L, LocalDate.of(2025, 11, 1), LocalDate.of(2026, 1, 31)))
				.thenReturn(List.of());

		CustomerRewardsResponse response = service.getCustomerRewards(1L, null, null);

		assertThat(response.getMonthlyRewards()).extracting(MonthlyRewards::getMonth)
				.containsExactly("2025-11", "2025-12", "2026-01");
	}

	@Test
	void onlyToGivenReportsThreeMonthsEndingWithThatMonth() {
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		when(transactionDao.findByCustomerIdAndDateRange(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 15)))
				.thenReturn(List.of());

		CustomerRewardsResponse response = service.getCustomerRewards(1L, null, LocalDate.of(2026, 6, 15));

		assertThat(response.getMonthlyRewards()).extracting(MonthlyRewards::getMonth)
				.containsExactly("2026-04", "2026-05", "2026-06");
	}

	@Test
	void onlyFromGivenReportsThreeMonthsStartingWithThatMonth() {
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		when(transactionDao.findByCustomerIdAndDateRange(1L, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 4, 30)))
				.thenReturn(List.of());

		CustomerRewardsResponse response = service.getCustomerRewards(1L, LocalDate.of(2026, 2, 10), null);

		assertThat(response.getMonthlyRewards()).extracting(MonthlyRewards::getMonth)
				.containsExactly("2026-02", "2026-03", "2026-04");
	}

	@Test
	void unknownCustomerIsRejected() {
		when(customerDao.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getCustomerRewards(99L, null, null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
		// The customer check must come first: no transaction query for an unknown customer.
		verify(transactionDao, never()).findByCustomerIdAndDateRange(any(), any(), any());
	}

	@Test
	void fromAfterToIsRejected() {
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));

		assertThatThrownBy(() -> service.getCustomerRewards(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1)))
				.isInstanceOf(InvalidDateRangeException.class);
	}

	@Test
	void periodLongerThanMaximumIsRejected() { // two years > the 366 day limit
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));

		assertThatThrownBy(() -> service.getCustomerRewards(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1)))
				.isInstanceOf(InvalidDateRangeException.class);
	}

	@Test
	void allCustomersIncludesCustomersWithoutTransactions() {
		LocalDate from = LocalDate.of(2026, 6, 1);
		LocalDate to = LocalDate.of(2026, 6, 30);
		when(customerDao.findAll()).thenReturn(List.of(ALICE, BOB));
		when(transactionDao.findByDateRange(from, to)).thenReturn(List.of(
				tx(1L, "120.00", LocalDate.of(2026, 6, 5)),
				tx(1L, "60.00", LocalDate.of(2026, 6, 6))));

		List<CustomerRewardsResponse> responses = service.getAllCustomerRewards(from, to);

		assertThat(responses).extracting(CustomerRewardsResponse::getCustomerId, CustomerRewardsResponse::getTotalPoints)
				.containsExactly(
						tuple(1L, 100L), // 90 + 10
						tuple(2L, 0L)); // Bob has no transactions but is still listed
	}

	/** Test helper: a transaction without id or description. */
	private static PurchaseTransaction tx(Long customerId, String amount, LocalDate date) {
		return PurchaseTransaction.builder()
				.customerId(customerId)
				.amount(new BigDecimal(amount))
				.transactionDate(date)
				.build();
	}
}

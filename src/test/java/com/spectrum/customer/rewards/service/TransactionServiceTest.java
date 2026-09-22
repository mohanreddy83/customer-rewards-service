package com.spectrum.customer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spectrum.customer.rewards.dao.CustomerDao;
import com.spectrum.customer.rewards.dao.TransactionDao;
import com.spectrum.customer.rewards.dto.TransactionRequest;
import com.spectrum.customer.rewards.dto.TransactionResponse;
import com.spectrum.customer.rewards.exception.ResourceNotFoundException;
import com.spectrum.customer.rewards.model.Customer;
import com.spectrum.customer.rewards.model.PurchaseTransaction;

/**
 * Unit tests for {@link TransactionService} with mocked DAOs. Cache eviction is verified in the
 * integration test because it needs the Spring proxy.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	private static final Customer ALICE = Customer.builder().customerId(1L).name("Alice").build();

	@Mock
	private CustomerDao customerDao;

	@Mock
	private TransactionDao transactionDao;

	private TransactionService service;

	@BeforeEach
	void setUp() {
		service = new TransactionService(customerDao, transactionDao, new RewardPointsCalculator());
	}

	@Test
	void createTransactionStoresPurchaseAndReturnsPoints() {
		TransactionRequest request = new TransactionRequest(1L, new BigDecimal("120.00"), LocalDate.of(2026, 8, 15),
				"Shoes");
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		// Simulate the database: echo the transaction back with a generated id.
		when(transactionDao.save(any())).thenAnswer(inv -> {
			PurchaseTransaction t = inv.getArgument(0);
			return PurchaseTransaction.builder().transactionId(42L).customerId(t.getCustomerId())
					.amount(t.getAmount()).transactionDate(t.getTransactionDate()).description(t.getDescription())
					.build();
		});

		TransactionResponse response = service.createTransaction(request);

		assertThat(response.getTransactionId()).isEqualTo(42L);
		assertThat(response.getRewardPoints()).isEqualTo(90); // $120 -> 2 x 20 + 50
		ArgumentCaptor<PurchaseTransaction> captor = ArgumentCaptor.forClass(PurchaseTransaction.class);
		// Check what was handed to the DAO, not just what came back.
		verify(transactionDao).save(captor.capture());
		assertThat(captor.getValue().getCustomerId()).isEqualTo(1L);
		assertThat(captor.getValue().getAmount()).isEqualByComparingTo("120.00");
		assertThat(captor.getValue().getDescription()).isEqualTo("Shoes");
	}

	@Test
	void createTransactionForUnknownCustomerIsRejected() {
		when(customerDao.findById(99L)).thenReturn(Optional.empty());
		TransactionRequest request = new TransactionRequest(99L, new BigDecimal("10.00"), LocalDate.of(2026, 8, 15),
				null);

		assertThatThrownBy(() -> service.createTransaction(request)).isInstanceOf(ResourceNotFoundException.class);
		verify(transactionDao, never()).save(any());
	}

	@Test
	void listsTransactionsWithPoints() {
		when(customerDao.findById(1L)).thenReturn(Optional.of(ALICE));
		when(transactionDao.findByCustomerId(1L)).thenReturn(List.of(
				PurchaseTransaction.builder().transactionId(1L).customerId(1L).amount(new BigDecimal("75.00"))
						.transactionDate(LocalDate.of(2026, 6, 1)).build()));

		List<TransactionResponse> responses = service.getTransactionsForCustomer(1L);

		assertThat(responses).singleElement().satisfies(r -> assertThat(r.getRewardPoints()).isEqualTo(25));
	}

	@Test
	void listingTransactionsOfUnknownCustomerIsRejected() {
		when(customerDao.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getTransactionsForCustomer(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}

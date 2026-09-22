package com.customer.rewards.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.customer.rewards.config.CacheConfig;
import com.customer.rewards.dao.CustomerDao;
import com.customer.rewards.dao.TransactionDao;
import com.customer.rewards.dto.TransactionRequest;
import com.customer.rewards.dto.TransactionResponse;
import com.customer.rewards.exception.ResourceNotFoundException;
import com.customer.rewards.model.PurchaseTransaction;

import lombok.RequiredArgsConstructor;

/**
 * Records and lists purchase transactions.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

	private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

	private final CustomerDao customerDao;
	private final TransactionDao transactionDao;
	private final RewardPointsCalculator calculator;

	/**
	 * Records a new purchase. Cached reward calculations are discarded because they are stale
	 * once a transaction has been added.
	 *
	 * @param request the purchase to record
	 * @return the stored purchase including its generated id and the points it earned
	 * @throws ResourceNotFoundException if the customer does not exist
	 */
	// Both caches are cleared entirely: a new transaction can change any customer's totals and any cached period.
	@Caching(evict = {
			@CacheEvict(cacheNames = CacheConfig.CUSTOMER_REWARDS, allEntries = true),
			@CacheEvict(cacheNames = CacheConfig.ALL_REWARDS, allEntries = true) })
	public TransactionResponse createTransaction(TransactionRequest request) {
		// Validate the customer here (rather than relying on the FK violation) to return a clean 404.
		if (customerDao.findById(request.getCustomerId()).isEmpty()) {
			throw new ResourceNotFoundException("Customer " + request.getCustomerId() + " not found");
		}
		PurchaseTransaction saved = transactionDao.save(PurchaseTransaction.builder()
				.customerId(request.getCustomerId())
				.amount(request.getAmount())
				.transactionDate(request.getTransactionDate())
				.description(request.getDescription())
				.build());
		log.info("Recorded transaction {} for customer {}", saved.getTransactionId(), saved.getCustomerId());
		return toResponse(saved);
	}

	/**
	 * Lists the purchases of a customer.
	 *
	 * @param customerId the customer identifier
	 * @return the customer's purchases, oldest first
	 * @throws ResourceNotFoundException if the customer does not exist
	 */
	public List<TransactionResponse> getTransactionsForCustomer(Long customerId) {
		if (customerDao.findById(customerId).isEmpty()) {
			throw new ResourceNotFoundException("Customer " + customerId + " not found");
		}
		return transactionDao.findByCustomerId(customerId).stream().map(this::toResponse).toList();
	}

	/**
	 * Converts a stored transaction into its API representation, adding the points it earns.
	 *
	 * @param transaction the stored transaction
	 * @return the response DTO
	 */
	private TransactionResponse toResponse(PurchaseTransaction transaction) {
		return TransactionResponse.builder()
				.transactionId(transaction.getTransactionId())
				.customerId(transaction.getCustomerId())
				.amount(transaction.getAmount())
				.transactionDate(transaction.getTransactionDate())
				.description(transaction.getDescription())
				.rewardPoints(calculator.calculatePoints(transaction.getAmount()))
				.build();
	}
}

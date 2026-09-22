package com.spectrum.customer.rewards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spectrum.customer.rewards.dao.TransactionDao;
import com.spectrum.customer.rewards.service.RewardsService;

/**
 * End-to-end tests: real controllers, services, cache and the seeded HSQLDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CustomerRewardsServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RewardsService rewardsService;

	@Autowired
	private CacheManager cacheManager;

	@MockitoSpyBean
	private TransactionDao transactionDao;

	/** The spy records calls across tests only if the context is reused; clear so each test counts from zero. */
	@BeforeEach
	void resetSpy() {
		clearInvocations(transactionDao);
	}

	@Test
	void contextLoads() {
		assertThat(cacheManager.getCacheNames()).contains("customerRewards", "allRewards");
	}

	// Expected numbers come from data.sql and are worked out in the README.
	@Test
	void aliceRewardsForDefaultPeriod() throws Exception {
		mockMvc.perform(get("/api/v1/rewards/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerName").value("Alice Johnson"))
				.andExpect(jsonPath("$.periodStart").value("2026-06-01"))
				.andExpect(jsonPath("$.periodEnd").value("2026-08-31"))
				.andExpect(jsonPath("$.monthlyRewards[0].month").value("2026-06"))
				.andExpect(jsonPath("$.monthlyRewards[0].points").value(115))
				.andExpect(jsonPath("$.monthlyRewards[1].points").value(250))
				.andExpect(jsonPath("$.monthlyRewards[2].points").value(160))
				.andExpect(jsonPath("$.totalPoints").value(525));
	}

	@Test
	void allCustomersRewardsForDefaultPeriod() throws Exception {
		mockMvc.perform(get("/api/v1/rewards"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].totalPoints").value(525))
				.andExpect(jsonPath("$[1].totalPoints").value(452))
				.andExpect(jsonPath("$[2].totalPoints").value(925))
				.andExpect(jsonPath("$[3].totalPoints").value(299))
				.andExpect(jsonPath("$[4].totalPoints").value(0));
	}

	@Test
	void explicitPeriodIncludesEarlierTransactions() throws Exception {
		mockMvc.perform(get("/api/v1/rewards/1").param("from", "2026-05-01").param("to", "2026-05-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyRewards.length()").value(1))
				.andExpect(jsonPath("$.totalPoints").value(450)); // 300 -> 2x200 + 50
	}

	@Test
	void secondCallIsServedFromCache() {
		LocalDate from = LocalDate.of(2026, 6, 1);
		LocalDate to = LocalDate.of(2026, 8, 31);

		var first = rewardsService.getCustomerRewards(1L, from, to);
		var second = rewardsService.getCustomerRewards(1L, from, to);

		// Same instance + a single DAO call proves the second result came from the cache.
		assertThat(second).isSameAs(first);
		verify(transactionDao, times(1)).findByCustomerIdAndDateRange(1L, from, to);
	}

	@Test
	void recordingTransactionEvictsCacheAndChangesRewards() throws Exception {
		// 1. Prime the cache: Eve has 0 points.
		mockMvc.perform(get("/api/v1/rewards/5")).andExpect(jsonPath("$.totalPoints").value(0));

		// 2. Record a purchase, which must evict the cached result.

		mockMvc.perform(post("/api/v1/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 5, "amount": 120.00, "transactionDate": "2026-08-20", "description": "Late buy"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rewardPoints").value(90));

		// 3. The next read reflects the new purchase (a stale cache would still say 0).
		mockMvc.perform(get("/api/v1/rewards/5"))
				.andExpect(jsonPath("$.totalPoints").value(90))
				.andExpect(jsonPath("$.monthlyRewards[2].transactionCount").value(2));
	}

	@Test
	void unknownCustomerIsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/rewards/99")).andExpect(status().isNotFound());
	}

	@Test
	void openApiDocumentIsPublished() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/rewards/{customerId}']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/transactions']").exists());
	}
}

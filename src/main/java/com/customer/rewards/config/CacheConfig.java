package com.customer.rewards.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's annotation driven caching ({@code @Cacheable}, {@code @CacheEvict}).
 *
 * <p>The Caffeine cache manager itself is configured through the {@code spring.cache.*} properties in
 * {@code application.properties} (maximum size and expiry). The cache names declared here must be kept in
 * sync with {@code spring.cache.cache-names} in that file.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

	/** Cache holding the rewards of a single customer for a period. */
	public static final String CUSTOMER_REWARDS = "customerRewards";

	/** Cache holding the rewards of all customers for a period. */
	public static final String ALL_REWARDS = "allRewards";
}

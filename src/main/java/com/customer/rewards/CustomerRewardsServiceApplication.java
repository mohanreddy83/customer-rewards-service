package com.customer.rewards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Customer Rewards service.
 *
 * <p>The service calculates reward points earned by customers for their purchases,
 * broken down per month and in total.</p>
 */
@SpringBootApplication
public class CustomerRewardsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerRewardsServiceApplication.class, args);
	}

}

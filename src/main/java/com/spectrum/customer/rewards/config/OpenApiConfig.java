package com.spectrum.customer.rewards.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Metadata for the generated OpenAPI document and Swagger UI.
 *
 * <p>The endpoints themselves are discovered by springdoc from the controllers and their
 * {@code @Operation} / {@code @ApiResponse} annotations; only the document header is defined here.</p>
 */
@Configuration
public class OpenApiConfig {

	/**
	 * @return the OpenAPI header (title, version, description) shown at the top of Swagger UI
	 */
	@Bean
	public OpenAPI customerRewardsOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Customer Rewards Service API")
				.version("v1")
				.description("Calculates retail reward points per customer, per month and in total. "
						+ "2 points per dollar over $100 and 1 point per dollar between $50 and $100 of each transaction."));
	}
}

package com.popcorn.demo.domain.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderConfigurationTest {

	@Test
	@DisplayName("Applies local profile settings")
	void localOrderProperties() {
		OrderConfiguration config = new OrderConfiguration();

		OrderProperties properties = config.localOrderProperties();

		assertThat(properties.getPaymentSuccessRate()).isEqualTo(1.0);
		assertThat(properties.getCancelableTimeout()).isEqualTo(Duration.ofMinutes(15));
		assertThat(properties.getValidation().getMaxStockQuantity()).isEqualTo(1000);
		assertThat(properties.getCache().getOrderDetailTtl()).isEqualTo(Duration.ofMinutes(1));
		assertThat(properties.getAsync().getEventRetryCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("Applies dev profile settings")
	void devOrderProperties() {
		OrderConfiguration config = new OrderConfiguration();

		OrderProperties properties = config.devOrderProperties();

		assertThat(properties.getPaymentSuccessRate()).isEqualTo(0.9);
		assertThat(properties.getValidation().getMaxStockQuantity()).isEqualTo(100);
		assertThat(properties.getCache().getOrderListTtl()).isEqualTo(Duration.ofMinutes(2));
	}

	@Test
	@DisplayName("Applies prod profile settings")
	void prodOrderProperties() {
		OrderConfiguration config = new OrderConfiguration();

		OrderProperties properties = config.prodOrderProperties();

		assertThat(properties.getPaymentSuccessRate()).isEqualTo(0.95);
		assertThat(properties.getValidation().getMaxStockQuantity()).isEqualTo(50);
		assertThat(properties.getValidation().getMaxOrderAmount()).isEqualTo(500000);
		assertThat(properties.getCache().getOrderDetailTtl()).isEqualTo(Duration.ofMinutes(10));
		assertThat(properties.getAsync().getEventRetryCount()).isEqualTo(5);
	}
}

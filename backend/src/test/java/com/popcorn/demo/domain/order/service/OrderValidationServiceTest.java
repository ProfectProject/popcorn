package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.popcorn.demo.domain.order.exception.OrderNotFoundException;

class OrderValidationServiceTest {

	@Test
	@DisplayName("Resolves storeId by popupId")
	void resolveStoreId_success() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderValidationService service = new OrderValidationService(jdbcTemplate);
		UUID popupId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();

		when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq(popupId))).thenReturn(storeId);

		assertThat(service.resolveStoreId(popupId)).isEqualTo(storeId);
	}

	@Test
	@DisplayName("Throws when popupId is missing or lookup fails")
	void resolveStoreId_failure() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderValidationService service = new OrderValidationService(jdbcTemplate);
		UUID popupId = UUID.randomUUID();

		assertThatThrownBy(() -> service.resolveStoreId(null))
				.isInstanceOf(OrderNotFoundException.class);

		when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq(popupId)))
				.thenThrow(new EmptyResultDataAccessException(1));

		assertThatThrownBy(() -> service.resolveStoreId(popupId))
				.isInstanceOf(OrderNotFoundException.class);
	}

	@Test
	@DisplayName("Returns true when async validation passes")
	void validateOrderAsync_success() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderValidationService service = new OrderValidationService(jdbcTemplate);
		UUID popupId = UUID.randomUUID();

		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1001L))).thenReturn(1);

		assertThat(service.validateOrderAsync(1001L, popupId, 5)).isTrue();
	}

	@Test
	@DisplayName("Returns false for invalid popup or quantity")
	void validateOrderAsync_invalidInput() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderValidationService service = new OrderValidationService(jdbcTemplate);

		assertThat(service.validateOrderAsync(1001L, null, 5)).isFalse();
		assertThat(service.validateOrderAsync(1001L, UUID.randomUUID(), 0)).isFalse();
	}

	@Test
	@DisplayName("Returns false when customer is invalid")
	void validateOrderAsync_invalidCustomer() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderValidationService service = new OrderValidationService(jdbcTemplate);

		assertThat(service.validateOrderAsync(-1L, UUID.randomUUID(), 5)).isFalse();
	}

	@Test
	@DisplayName("Throws when storeId lookup returns null")
	void resolveStoreId_nullStoreId() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderValidationService service = new OrderValidationService(jdbcTemplate);
		UUID popupId = UUID.randomUUID();

		when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq(popupId))).thenReturn(null);

		assertThatThrownBy(() -> service.resolveStoreId(popupId))
				.isInstanceOf(OrderNotFoundException.class);
	}
}

package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class OrderItemPriceServiceImplTest {

	@Test
	@DisplayName("스케줄 가격 조회는 존재 여부와 가격 조회를 수행한다")
	void findSessionOptionPrice() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderItemPriceServiceImpl service = new OrderItemPriceServiceImpl(jdbcTemplate);
		UUID scheduleId = UUID.randomUUID();

		when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(scheduleId))).thenReturn(1);
		when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(scheduleId))).thenReturn(12000);

		Optional<Integer> price = service.findSessionOptionPrice(scheduleId);
		assertThat(price).contains(12000);
	}

	@Test
	@DisplayName("굿즈 가격 조회는 결과가 없으면 비어있는 Optional을 반환한다")
	void findMerchVariantPrice_empty() {
		JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
		OrderItemPriceServiceImpl service = new OrderItemPriceServiceImpl(jdbcTemplate);
		UUID goodsId = UUID.randomUUID();

		when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(goodsId))).thenReturn(null);

		Optional<Integer> price = service.findMerchVariantPrice(goodsId);
		assertThat(price).isEmpty();
	}
}

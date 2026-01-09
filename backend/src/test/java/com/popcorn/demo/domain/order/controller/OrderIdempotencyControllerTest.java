package com.popcorn.demo.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.cache.IdempotencyCacheStats;
import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.common.dto.BaseResponse;

class OrderIdempotencyControllerTest {

	@Test
	@DisplayName("Idempotency endpoints return success responses")
	void idempotencyEndpointsReturnSuccess() {
		IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
		OrderIdempotencyController controller = new OrderIdempotencyController(idempotencyService);

		IdempotencyCacheStats stats = IdempotencyCacheStats.builder()
				.hitCount(10)
				.missCount(2)
				.hitRate(0.83)
				.totalLoadTime(1000)
				.evictionCount(1)
				.cacheSize(3)
				.inProgressRequestCount(0)
				.newRequestCount(12)
				.cacheHitCount(8)
				.concurrentRequestCount(1)
				.operationErrorCount(0)
				.build();
		when(idempotencyService.getCacheStats()).thenReturn(stats);

		ResponseEntity<BaseResponse<IdempotencyCacheStats>> statsResponse = controller.getCacheStats();
		assertThat(statsResponse.getBody().getData().getCacheSize()).isEqualTo(3);

		ResponseEntity<String> textResponse = controller.getCacheStatsText();
		assertThat(textResponse.getBody()).isNotBlank();

		ResponseEntity<BaseResponse<String>> clearResponse = controller.clearAllCache();
		assertThat(clearResponse.getBody().getData()).isNotBlank();
		verify(idempotencyService).clearCache();

		ResponseEntity<BaseResponse<String>> invalidateResponse = controller.invalidateKey("idem-key");
		assertThat(invalidateResponse.getBody().getData()).isNotBlank();
		verify(idempotencyService).invalidateKey("idem-key");

		ResponseEntity<BaseResponse<String>> healthResponse = controller.healthCheck();
		assertThat(healthResponse.getBody().getData()).isNotBlank();
	}

	@Test
	@DisplayName("Invalid idempotency key throws exception")
	void invalidateKeyRejectsBlankKey() {
		IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
		OrderIdempotencyController controller = new OrderIdempotencyController(idempotencyService);

		assertThatThrownBy(() -> controller.invalidateKey("   "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}

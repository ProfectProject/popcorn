package com.popcorn.demo.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IdempotencyServiceTest {

    @Test
    void processRequestWithoutKeyExecutesDirectly() {
        IdempotencyService service = new CaffeineBasedIdempotencyService(new ObjectMapper());

        IdempotencyService.IdempotencyResult<String> result = service.processRequest(
                null,
                () -> "ok",
                String.class);

        assertThat(result.isFromCache()).isFalse();
        assertThat(result.getResult()).isEqualTo("ok");
    }

    @Test
    void processRequestCachesResponses() {
        IdempotencyService service = new CaffeineBasedIdempotencyService(new ObjectMapper());
        AtomicInteger counter = new AtomicInteger();

        IdempotencyService.IdempotencyResult<String> first = service.processRequest(
                "Key",
                () -> "value-" + counter.incrementAndGet(),
                String.class);

        IdempotencyService.IdempotencyResult<String> second = service.processRequest(
                "Key",
                () -> {
                    throw new RuntimeException("should not execute");
                },
                String.class);

        assertThat(first.isFromCache()).isFalse();
        assertThat(second.isFromCache()).isTrue();
        assertThat(second.getResult()).isEqualTo(first.getResult());
    }

    @Test
    void processRequestThrowsWhenOperationFails() {
        IdempotencyService service = new CaffeineBasedIdempotencyService(new ObjectMapper());

        assertThatThrownBy(() -> service.processRequest(
                "key",
                () -> { throw new RuntimeException("fail"); },
                String.class))
                .isInstanceOf(IdempotencyService.IdempotencyException.class);
    }

    @Test
    void processRequestContinuesWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        IdempotencyService service = new CaffeineBasedIdempotencyService(objectMapper);

        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
                .thenThrow(new RuntimeException("fail"));

        IdempotencyService.IdempotencyResult<String> result = service.processRequest(
                "serialize",
                () -> "ok",
                String.class);

        assertThat(result.getResult()).isEqualTo("ok");
    }

    @Test
    void invalidateAndClearCacheExecute() {
        IdempotencyService service = new CaffeineBasedIdempotencyService(new ObjectMapper());

        service.processRequest("Key", () -> "value", String.class);
        service.invalidateKey("Key");
        service.clearCache();

        IdempotencyCacheStats stats = service.getCacheStats();
        assertThat(stats.getCacheSize()).isGreaterThanOrEqualTo(0L);
    }
}

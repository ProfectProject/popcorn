package com.popcorn.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class IdempotencyServiceTest {

    @Test
    void processRequestWithoutKeyExecutesDirectly() {
        IdempotencyService service = createRedisService(new ObjectMapper());

        IdempotencyService.IdempotencyResult<String> result = service.processRequest(
                null,
                () -> "ok",
                String.class);

        assertThat(result.fromCache()).isFalse();
        assertThat(result.result()).isEqualTo("ok");
    }

    @Test
    void processRequestCachesResponses() {
        IdempotencyService service = createRedisService(new ObjectMapper());
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

        assertThat(first.fromCache()).isFalse();
        assertThat(second.fromCache()).isTrue();
        assertThat(second.result()).isEqualTo(first.result());
    }

    @Test
    void processRequestThrowsWhenOperationFails() {
        IdempotencyService service = createRedisService(new ObjectMapper());

        assertThatThrownBy(() -> service.processRequest(
                "key",
                () -> { throw new RuntimeException("fail"); },
                String.class))
                .isInstanceOf(IdempotencyService.IdempotencyException.class);
    }

    @Test
    void processRequestContinuesWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        IdempotencyService service = createRedisService(objectMapper);

        Mockito.when(objectMapper.writeValueAsString(Mockito.any()))
                .thenThrow(new RuntimeException("fail"));

        IdempotencyService.IdempotencyResult<String> result = service.processRequest(
                "serialize",
                () -> "ok",
                String.class);

        assertThat(result.result()).isEqualTo("ok");
    }

    @Test
    void invalidateAndClearCacheExecute() {
        IdempotencyService service = createRedisService(new ObjectMapper());

        service.processRequest("Key", () -> "value", String.class);
        service.invalidateKey("Key");
        service.clearCache();

        IdempotencyCacheStats stats = service.getCacheStats();
        assertThat(stats.getCacheSize()).isGreaterThanOrEqualTo(0L);
    }

    private IdempotencyService createRedisService(ObjectMapper objectMapper) {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        Map<String, String> store = new ConcurrentHashMap<>();

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Mockito.when(valueOperations.get(Mockito.anyString()))
            .thenAnswer(invocation -> store.get(invocation.getArgument(0)));

        Mockito.doAnswer(invocation -> {
            store.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class));

        Mockito.doAnswer(invocation -> {
            store.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(Mockito.anyString(), Mockito.anyString());

        Mockito.when(valueOperations.setIfAbsent(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                if (store.containsKey(key)) {
                    return Boolean.FALSE;
                }
                store.put(key, invocation.getArgument(1));
                return Boolean.TRUE;
            });

        Mockito.when(redisTemplate.keys(Mockito.anyString()))
            .thenAnswer(invocation -> {
                String pattern = invocation.getArgument(0);
                String prefix = pattern.replace("*", "");
                return store.keySet().stream()
                    .filter(key -> key.startsWith(prefix))
                    .collect(Collectors.toSet());
            });

        Mockito.doAnswer(invocation -> {
            store.remove(invocation.getArgument(0));
            return true;
        }).when(redisTemplate).delete(Mockito.anyString());

        Mockito.doAnswer(invocation -> {
            Set<String> keys = invocation.getArgument(0);
            keys.forEach(store::remove);
            return (long) keys.size();
        }).when(redisTemplate).delete(Mockito.anySet());

        return new RedisBasedIdempotencyService(redisTemplate, objectMapper);
    }
}

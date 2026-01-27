package com.popcorn.store.inventory.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryEventIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private InventoryEventIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new InventoryEventIdempotencyService(redisTemplate);
    }

    @Test
    @DisplayName("새로운 이벤트는 처리되어야 한다")
    void registerEvent_shouldReturnTrueWhenNew() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        boolean result = idempotencyService.registerEvent("evt-1", UUID.randomUUID(), "goods");

        assertThat(result).isTrue();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("1"), durationCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith("inventory:event:goods:");
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    @DisplayName("중복 이벤트는 false를 반환한다")
    void registerEvent_shouldReturnFalseWhenDuplicate() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        boolean result = idempotencyService.registerEvent("evt-2", UUID.randomUUID(), "schedule");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("eventId가 없으면 orderId를 키로 사용한다")
    void registerEvent_shouldFallbackToOrderId() {
        UUID orderId = UUID.randomUUID();
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        boolean result = idempotencyService.registerEvent(null, orderId, "schedule");

        assertThat(result).isTrue();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("1"), any(Duration.class));
        assertThat(keyCaptor.getValue()).contains(orderId.toString());
    }

    @Test
    @DisplayName("scope가 비어있으면 예외를 던진다")
    void registerEvent_shouldThrowWhenScopeMissing() {
        assertThatThrownBy(() -> idempotencyService.registerEvent("evt", UUID.randomUUID(), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.popcorn.store.inventory.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryRedisHoldServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisScript<Long> holdStockScript;

    @Mock
    private RedisScript<Long> releaseHoldScript;

    private InventoryRedisHoldService holdService;

    @BeforeEach
    void setUp() {
        holdService = new InventoryRedisHoldService(redisTemplate, holdStockScript, releaseHoldScript);
    }

    @Test
    void holdAvailability_shouldReturnTrueWhenScriptSucceeds() {
        UUID orderId = UUID.randomUUID();
        String availabilityKey = "goods_avail:popup:goods";

        lenient().when(redisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn(1L);

        boolean result = holdService.holdAvailability(orderId, availabilityKey, 3);

        assertThat(result).isTrue();

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(holdStockScript), eq(List.of(availabilityKey)), argsCaptor.capture());

        Object[] args = argsCaptor.getValue();
        assertThat(args).hasSizeGreaterThanOrEqualTo(3);
        assertThat(args[0]).isEqualTo(holdService.buildHoldKey(orderId));
        assertThat(args[1]).isEqualTo("3");
        assertThat(args[2]).isEqualTo(String.valueOf(Duration.ofMinutes(30).toMillis()));
    }

    @Test
    void holdAvailability_shouldThrowWhenQuantityInvalid() {
        assertThatThrownBy(() -> holdService.holdAvailability(UUID.randomUUID(), "key", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releaseHold_shouldReturnTrueWhenScriptReturnsPositive() {
        UUID orderId = UUID.randomUUID();
        String holdKey = holdService.buildHoldKey(orderId);
        when(redisTemplate.execute(eq(releaseHoldScript), eq(List.of(holdKey)))).thenReturn(1L);

        boolean result = holdService.releaseHold(orderId);

        assertThat(result).isTrue();
        verify(redisTemplate).execute(eq(releaseHoldScript), eq(List.of(holdKey)));
    }

    @Test
    void releaseHold_shouldThrowWhenOrderIdMissing() {
        assertThatThrownBy(() -> holdService.releaseHold(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

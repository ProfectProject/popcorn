package com.popcorn.store.inventory.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
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
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryRedisHoldServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisScript<Long> holdScheduleScript;

    @Mock
    private RedisScript<Long> holdGoodsScript;

    @Mock
    private RedisScript<Long> holdBothScript;

    @Mock
    private RedisScript<Long> releaseHoldScript;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private InventoryRedisHoldService holdService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        holdService = new InventoryRedisHoldService(
                redisTemplate,
                holdScheduleScript,
                holdGoodsScript,
                holdBothScript,
                releaseHoldScript,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("HOLD_SCHEDULE 실행 후 lookup 키 생성")
    void holdSchedule_shouldRegisterLookup() {
        UUID orderId = UUID.randomUUID();
        UUID popupId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        when(redisTemplate.execute(eq(holdScheduleScript), anyList(), any(Object[].class)))
                .thenReturn(1L);

        InventoryRedisHoldService.HoldResult result =
                holdService.holdSchedule(orderId, popupId, scheduleId, 2);


        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("hold_lookup:" + orderId), eq(popupId.toString()), durationCaptor.capture());
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("HOLD_GOODS는 goods 키와 qtys를 전달한다")
    void holdGoods_shouldUseGoodsScript() {
        UUID orderId = UUID.randomUUID();
        UUID popupId = UUID.randomUUID();
        InventoryRedisHoldService.GoodsHoldItem item1 =
                new InventoryRedisHoldService.GoodsHoldItem(UUID.randomUUID(), 2);
        InventoryRedisHoldService.GoodsHoldItem item2 =
                new InventoryRedisHoldService.GoodsHoldItem(UUID.randomUUID(), 1);

        when(redisTemplate.execute(eq(holdGoodsScript), anyList(), any(Object[].class)))
                .thenReturn(1L);

        InventoryRedisHoldService.HoldResult result =
                holdService.holdGoods(orderId, popupId, List.of(item1, item2));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(holdGoodsScript), keysCaptor.capture(), argsCaptor.capture());
        List<String> keys = keysCaptor.getValue();
        assertThat(keys).hasSize(2);
        Object[] args = argsCaptor.getValue();
        assertThat(args).contains(String.valueOf(2), String.valueOf(1));
        verify(valueOperations).set(eq("hold_lookup:" + orderId), eq(popupId.toString()), any(Duration.class));
    }

    @Test
    @DisplayName("releaseHold는 lookup을 통해 popupId를 찾아 스크립트 실행")
    void releaseHold_shouldCallReleaseScript() {
        UUID orderId = UUID.randomUUID();
        UUID popupId = UUID.randomUUID();
        when(valueOperations.get("hold_lookup:" + orderId)).thenReturn(popupId.toString());
        when(redisTemplate.execute(eq(releaseHoldScript), anyList())).thenReturn(1L);

        InventoryRedisHoldService.HoldResult result = holdService.releaseHold(orderId);

        assertThat(result.isSuccess()).isTrue();
        String expectedKey = String.format("hold:{%s}:%s", popupId, orderId);
        verify(redisTemplate).execute(eq(releaseHoldScript), eq(List.of(expectedKey)));
        verify(redisTemplate).delete("hold_lookup:" + orderId);
    }

    @Test
    @DisplayName("releaseHold는 lookup이 없으면 멱등 성공 처리")
    void releaseHold_shouldTreatMissingLookupAsSuccess() {
        UUID orderId = UUID.randomUUID();
        when(valueOperations.get("hold_lookup:" + orderId)).thenReturn(null);

        InventoryRedisHoldService.HoldResult result = holdService.releaseHold(orderId);

        assertThat(result.isSuccess()).isTrue();
        verify(redisTemplate).delete("hold_lookup:" + orderId);
    }
}

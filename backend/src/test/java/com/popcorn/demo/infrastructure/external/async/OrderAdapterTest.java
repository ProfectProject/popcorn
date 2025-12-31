package com.popcorn.demo.infrastructure.external.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.domain.order.service.OrderService;

/**
 * OrderAdapter 단위 테스트
 *
 * Clean Architecture의 Infrastructure Layer 테스트
 * - ProcessOrderPort의 구현체 테스트
 * - 처리 작업의 정상 동작 검증
 * - CompletableFuture를 통한 작업 결과 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 어댑터 테스트")
class OrderAdapterTest {

    @Mock
    private OrderService orderService;

    private OrderAdapter orderAdapter;

    @BeforeEach
    void setUp() {
        orderAdapter = new OrderAdapter(orderService);
    }

    @Test
    @DisplayName("주문 후처리 작업 - 정상 처리")
    void processOrderPostActions_ValidOrderId_ReturnsCompletedFuture() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long orderId = 101L;
        CompletableFuture<Void> expectedFuture = CompletableFuture.completedFuture(null);
        when(orderService.processOrderPostActions(orderId)).thenReturn(expectedFuture);

        // when
        CompletableFuture<Void> result = orderAdapter.processOrderPostActions(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();

        // 실제로 완료를 기다려보기 (타임아웃 설정)
        result.get(1, TimeUnit.SECONDS);

        verify(orderService, times(1)).processOrderPostActions(orderId);
    }

    @Test
    @DisplayName("주문 후처리 작업 - 여러 주문 동시 처리")
    void processOrderPostActions_MultipleOrders_ProcessesConcurrently() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long orderId1 = 101L;
        Long orderId2 = 102L;
        Long orderId3 = 103L;

        CompletableFuture<Void> future1 = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future2 = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future3 = CompletableFuture.completedFuture(null);

        when(orderService.processOrderPostActions(orderId1)).thenReturn(future1);
        when(orderService.processOrderPostActions(orderId2)).thenReturn(future2);
        when(orderService.processOrderPostActions(orderId3)).thenReturn(future3);

        // when
        CompletableFuture<Void> result1 = orderAdapter.processOrderPostActions(orderId1);
        CompletableFuture<Void> result2 = orderAdapter.processOrderPostActions(orderId2);
        CompletableFuture<Void> result3 = orderAdapter.processOrderPostActions(orderId3);

        // then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();

        // 모든 작업이 완료되었는지 확인
        CompletableFuture.allOf(result1, result2, result3).get(2, TimeUnit.SECONDS);

        verify(orderService, times(1)).processOrderPostActions(orderId1);
        verify(orderService, times(1)).processOrderPostActions(orderId2);
        verify(orderService, times(1)).processOrderPostActions(orderId3);
    }

    @Test
    @DisplayName("주문 후처리 작업 - null 주문 ID로 호출 시 예외 발생")
    void processOrderPostActions_NullOrderId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> orderAdapter.processOrderPostActions(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 후처리 작업 - 0 또는 음수 주문 ID로 호출 시 예외 발생")
    void processOrderPostActions_InvalidOrderId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> orderAdapter.processOrderPostActions(0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> orderAdapter.processOrderPostActions(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 후처리 작업 - 서비스에서 예외 발생 시 처리")
    void processOrderPostActions_ServiceThrowsException_ReturnsExceptionallyCompletedFuture() {
        // given
        Long orderId = 101L;
        CompletableFuture<Void> exceptionalFuture = new CompletableFuture<>();
        exceptionalFuture.completeExceptionally(new RuntimeException("처리 중 오류 발생"));

        when(orderService.processOrderPostActions(orderId)).thenReturn(exceptionalFuture);

        // when
        CompletableFuture<Void> result = orderAdapter.processOrderPostActions(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isTrue();

        verify(orderService, times(1)).processOrderPostActions(orderId);
    }

    @Test
    @DisplayName("주문 검증 처리 - 정상 검증 통과")
    void validateOrder_ValidInput_ReturnsCompletedFuture() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long userId = 1001L;
        Long productId = 1L;
        int qty = 2;

        CompletableFuture<Boolean> expectedFuture = CompletableFuture.completedFuture(true);
        when(orderService.validateOrderAsync(userId, productId, qty)).thenReturn(expectedFuture);

        // when
        CompletableFuture<Boolean> result = orderAdapter.validateOrder(userId, productId, qty);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.get(1, TimeUnit.SECONDS)).isTrue();

        verify(orderService, times(1)).validateOrderAsync(userId, productId, qty);
    }

    @Test
    @DisplayName("주문 검증 처리 - 검증 실패")
    void validateOrder_ValidationFails_ReturnsFalse() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long userId = 1001L;
        Long productId = 999L; // 존재하지 않는 상품
        int qty = 2;

        CompletableFuture<Boolean> expectedFuture = CompletableFuture.completedFuture(false);
        when(orderService.validateOrderAsync(userId, productId, qty)).thenReturn(expectedFuture);

        // when
        CompletableFuture<Boolean> result = orderAdapter.validateOrder(userId, productId, qty);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.get(1, TimeUnit.SECONDS)).isFalse();

        verify(orderService, times(1)).validateOrderAsync(userId, productId, qty);
    }

    @Test
    @DisplayName("어댑터 초기화 - OrderService가 null인 경우 예외 발생")
    void constructor_NullService_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> new OrderAdapter(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("어댑터 초기화 - 정상 초기화")
    void constructor_ValidService_CreatesAdapter() {
        // when
        OrderAdapter adapter = new OrderAdapter(orderService);

        // then
        assertThat(adapter).isNotNull();
    }

    @Test
    @DisplayName("장시간 실행되는 작업 - 타임아웃 테스트")
    void processOrderPostActions_LongRunningTask_HandlesTimeout() {
        // given
        Long orderId = 101L;
        CompletableFuture<Void> longRunningFuture = new CompletableFuture<>();

        // 5초 후에 완료되는 작업 시뮬레이션
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
                .execute(() -> longRunningFuture.complete(null));

        when(orderService.processOrderPostActions(orderId)).thenReturn(longRunningFuture);

        // when
        CompletableFuture<Void> result = orderAdapter.processOrderPostActions(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isFalse(); // 아직 완료되지 않음

        // 짧은 타임아웃으로 테스트 - 실제로는 완료되지 않을 것
        assertThatThrownBy(() -> result.get(100, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        verify(orderService, times(1)).processOrderPostActions(orderId);
    }
}
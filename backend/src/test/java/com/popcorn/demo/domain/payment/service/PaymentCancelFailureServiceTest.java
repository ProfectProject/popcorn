package com.popcorn.demo.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.popcorn.demo.domain.payment.entity.PaymentCancelFailureQueue;
import com.popcorn.demo.domain.payment.event.PaymentCancelFailedEvent;
import com.popcorn.demo.domain.payment.repository.JpaPaymentCancelFailureQueueRepository;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelRequest;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelResponse;
import com.popcorn.demo.domain.payment.toss.TossPaymentsClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCancelFailureService 테스트")
class PaymentCancelFailureServiceTest {

    @Mock
    private JpaPaymentCancelFailureQueueRepository failureQueueRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentCancelFailureService paymentCancelFailureService;

    private PaymentCancelFailedEvent testEvent;
    private PaymentCancelFailureQueue testQueue;
    private UUID orderId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testEvent = new PaymentCancelFailedEvent(
            this,
            orderId,
            paymentId,
            "test_payment_key",
            "사용자 취소",
            "네트워크 오류",
            10000,
            LocalDateTime.now(),
            1
        );

        testQueue = PaymentCancelFailureQueue.builder()
            .orderId(orderId)
            .paymentId(paymentId)
            .paymentKey("test_payment_key")
            .cancelReason("사용자 취소")
            .failureReason("네트워크 오류")
            .amount(10000)
            .attemptCount(1)
            .nextRetryAt(LocalDateTime.now().plusMinutes(1))
            .build();
    }

    @Test
    @DisplayName("실패 큐에 결제 취소 실패 사항을 저장한다")
    void addToFailureQueue_Success() {
        // Given
        when(failureQueueRepository.existsByOrderIdAndStatusIn(any(UUID.class), anyList()))
            .thenReturn(false);

        // When
        paymentCancelFailureService.addToFailureQueue(testEvent);

        // Then
        ArgumentCaptor<PaymentCancelFailureQueue> queueCaptor = ArgumentCaptor.forClass(PaymentCancelFailureQueue.class);
        verify(failureQueueRepository).save(queueCaptor.capture());

        PaymentCancelFailureQueue savedQueue = queueCaptor.getValue();
        assertThat(savedQueue.getOrderId()).isEqualTo(orderId);
        assertThat(savedQueue.getPaymentId()).isEqualTo(paymentId);
        assertThat(savedQueue.getPaymentKey()).isEqualTo("test_payment_key");
        assertThat(savedQueue.getCancelReason()).isEqualTo("사용자 취소");
        assertThat(savedQueue.getFailureReason()).isEqualTo("네트워크 오류");
        assertThat(savedQueue.getAmount()).isEqualTo(10000);
        assertThat(savedQueue.getAttemptCount()).isEqualTo(1);
        assertThat(savedQueue.getNextRetryAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("이미 실패 큐에 등록된 주문인 경우 중복 저장하지 않는다")
    void addToFailureQueue_AlreadyQueued() {
        // Given
        when(failureQueueRepository.existsByOrderIdAndStatusIn(
            orderId,
            List.of(
                PaymentCancelFailureQueue.QueueStatus.PENDING,
                PaymentCancelFailureQueue.QueueStatus.RETRYING
            )
        )).thenReturn(true);

        // When
        paymentCancelFailureService.addToFailureQueue(testEvent);

        // Then
        verify(failureQueueRepository, never()).save(any());
    }

    @Test
    @DisplayName("재시도 가능한 큐가 없는 경우 아무것도 처리하지 않는다")
    void processRetriableQueues_EmptyList() {
        // Given
        when(failureQueueRepository.findRetriableQueues(any(LocalDateTime.class)))
            .thenReturn(List.of());

        // When
        paymentCancelFailureService.processRetriableQueues();

        // Then
        verify(tossPaymentsClient, never()).cancel(anyString(), any());
    }

    @Test
    @DisplayName("재시도 가능한 큐들을 처리한다")
    void processRetriableQueues_Success() {
        // Given
        List<PaymentCancelFailureQueue> retriableQueues = List.of(testQueue);
        when(failureQueueRepository.findRetriableQueues(any(LocalDateTime.class)))
            .thenReturn(retriableQueues);

        TossPaymentsCancelResponse mockResponse = new TossPaymentsCancelResponse();
        mockResponse.setTotalAmount(10000);
        when(tossPaymentsClient.cancel(anyString(), any(TossPaymentsCancelRequest.class)))
            .thenReturn(mockResponse);

        // When
        paymentCancelFailureService.processRetriableQueues();

        // Then
        verify(tossPaymentsClient).cancel(
            eq("test_payment_key"),
            any(TossPaymentsCancelRequest.class));
        verify(failureQueueRepository, times(2)).save(any(PaymentCancelFailureQueue.class));
    }

    @Test
    @DisplayName("재시도 중 오류 발생 시 실패 처리를 한다")
    void processRetriableQueues_RetryFailure() {
        // Given
        List<PaymentCancelFailureQueue> retriableQueues = List.of(testQueue);
        when(failureQueueRepository.findRetriableQueues(any(LocalDateTime.class)))
            .thenReturn(retriableQueues);

        when(tossPaymentsClient.cancel(anyString(), any(TossPaymentsCancelRequest.class)))
            .thenThrow(new RuntimeException("토스 API 오류"));

        // When
        paymentCancelFailureService.processRetriableQueues();

        // Then
        verify(failureQueueRepository, times(2)).save(any(PaymentCancelFailureQueue.class));
        // 첫 번째 save: setRetrying(), 두 번째 save: handleRetryFailure()
    }

    @Test
    @DisplayName("재시도 처리 자체에서 예외 발생 시 예외를 잡아서 처리한다")
    void processRetriableQueues_ExceptionInRetry() {
        // Given
        PaymentCancelFailureQueue mockQueue = PaymentCancelFailureQueue.builder()
            .orderId(orderId)
            .paymentId(paymentId)
            .paymentKey("test_payment_key")
            .cancelReason("사용자 취소")
            .build();

        List<PaymentCancelFailureQueue> retriableQueues = List.of(mockQueue);
        when(failureQueueRepository.findRetriableQueues(any(LocalDateTime.class)))
            .thenReturn(retriableQueues);

        // TossPaymentsClient에서 예외 발생하도록 설정
        when(tossPaymentsClient.cancel(anyString(), any(TossPaymentsCancelRequest.class)))
            .thenThrow(new RuntimeException("토스 API 오류"));

        // When
        paymentCancelFailureService.processRetriableQueues();

        // Then - 예외가 발생하더라도 프로세스가 계속되어야 함
        verify(failureQueueRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("특정 주문의 실패 큐 상태를 조회한다")
    void getFailureQueuesForOrder() {
        // Given
        List<PaymentCancelFailureQueue> expectedQueues = List.of(testQueue);
        when(failureQueueRepository.findByPaymentId(orderId))
            .thenReturn(expectedQueues);

        // When
        List<PaymentCancelFailureQueue> result = paymentCancelFailureService.getFailureQueuesForOrder(orderId);

        // Then
        assertThat(result).isEqualTo(expectedQueues);
        verify(failureQueueRepository).findByPaymentId(orderId);
    }

    @Test
    @DisplayName("오래된 실패 큐들을 조회한다")
    void getStuckQueues() {
        // Given
        List<PaymentCancelFailureQueue> expectedQueues = List.of(testQueue);
        when(failureQueueRepository.findStuckQueues(any(LocalDateTime.class)))
            .thenReturn(expectedQueues);

        // When
        List<PaymentCancelFailureQueue> result = paymentCancelFailureService.getStuckQueues();

        // Then
        assertThat(result).isEqualTo(expectedQueues);

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(failureQueueRepository).findStuckQueues(thresholdCaptor.capture());

        LocalDateTime threshold = thresholdCaptor.getValue();
        LocalDateTime expectedThreshold = LocalDateTime.now().minusHours(24);
        // 24시간 전후 1분 이내의 오차 허용
        assertThat(threshold).isBetween(
            expectedThreshold.minusMinutes(1),
            expectedThreshold.plusMinutes(1)
        );
    }

    @Test
    @DisplayName("최대 시도 횟수 초과 시 최종 실패로 처리한다")
    void handleRetryFailure_FinalFailure() {
        // Given
        PaymentCancelFailureQueue queueWithMaxAttempts = PaymentCancelFailureQueue.builder()
            .orderId(orderId)
            .paymentId(paymentId)
            .paymentKey("test_payment_key")
            .cancelReason("사용자 취소")
            .attemptCount(4) // maxAttempts는 기본값 5이므로 4번째 시도로 설정
            .maxAttempts(5)
            .build();

        List<PaymentCancelFailureQueue> retriableQueues = List.of(queueWithMaxAttempts);
        when(failureQueueRepository.findRetriableQueues(any(LocalDateTime.class)))
            .thenReturn(retriableQueues);

        when(tossPaymentsClient.cancel(anyString(), any(TossPaymentsCancelRequest.class)))
            .thenThrow(new RuntimeException("최종 실패"));

        // When
        paymentCancelFailureService.processRetriableQueues();

        // Then
        verify(failureQueueRepository, times(2)).save(any(PaymentCancelFailureQueue.class));
        // 최종 실패 이벤트 로그가 기록되었는지는 로그로만 확인 가능
    }

    @Test
    @DisplayName("null 이벤트로 실패 큐 추가 시도 시 NullPointerException 발생")
    void addToFailureQueue_NullEvent() {
        // When & Then
        assertThatThrownBy(() -> paymentCancelFailureService.addToFailureQueue(null))
            .isInstanceOf(NullPointerException.class);
    }
}
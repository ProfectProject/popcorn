package com.popcorn.payment.event

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("PaymentEventListener 단위 테스트")
class PaymentEventListenerUnitTest {

    private val paymentEventPublisher = mockk<PaymentEventPublisherImpl>()
    private lateinit var paymentEventListener: PaymentEventListener

    private val paymentId = UUID.randomUUID()
    private val orderId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        paymentEventListener = PaymentEventListener(paymentEventPublisher)

        // PaymentEventPublisher의 메서드들을 모두 성공으로 설정
        coEvery { paymentEventPublisher.publishQrCodeGenerationRequested(any(), any(), any(), any()) } just Runs
        every { paymentEventPublisher.publishAsync(any()) } just Runs
    }

    @Test
    @DisplayName("결제 생성 이벤트 처리 성공")
    fun `handlePaymentCreated should process event successfully`() {
        // Given
        val event = PaymentCreatedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-001",
            amount = 10000,
            paymentMethod = "CARD",
            customerId = 1L
        )

        // When & Then - 예외 없이 처리되어야 함
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentCreated(event) }
        }
    }

    @Test
    @DisplayName("결제 승인 이벤트 처리 - QR 코드 생성 요청 이벤트 발행 확인")
    fun `handlePaymentApproved should publish QR generation event`() {
        // Given
        val event = PaymentApprovedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-002",
            amount = 15000,
            paymentMethod = "CARD",
            paymentKey = "test_key",
            approvedAt = LocalDateTime.now(),
            customerId = 2L
        )

        // When
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentApproved(event) }
        }

        // Then
        coVerify(exactly = 1) {
            paymentEventPublisher.publishQrCodeGenerationRequested(
                paymentId = paymentId,
                orderId = orderId,
                orderNo = "ORDER-002",
                customerId = 2L
            )
        }

        // 재고 차감 확정 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<InventoryConfirmationRequestedEvent> { inventoryEvent ->
                    inventoryEvent.paymentId == paymentId &&
                    inventoryEvent.orderId == orderId &&
                    inventoryEvent.actionType == "CONFIRM"
                }
            )
        }
    }

    @Test
    @DisplayName("결제 실패 이벤트 처리 - 재고 복구 이벤트 발행")
    fun `handlePaymentFailed should publish inventory restore event`() {
        // Given
        val event = PaymentFailedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-FAILED",
            amount = 20000,
            paymentMethod = "TRANSFER",
            failureReason = "카드 한도 초과",
            customerId = 3L
        )

        // When
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentFailed(event) }
        }

        // Then - 재고 복구 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<InventoryConfirmationRequestedEvent> { inventoryEvent ->
                    inventoryEvent.paymentId == paymentId &&
                    inventoryEvent.orderId == orderId &&
                    inventoryEvent.actionType == "RESTORE"
                }
            )
        }

        // 주문 상태 실패 변경 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<OrderStatusUpdateRequestedEvent> { orderEvent ->
                    orderEvent.paymentId == paymentId &&
                    orderEvent.orderId == orderId &&
                    orderEvent.newStatus == "FAILED" &&
                    orderEvent.reason == "카드 한도 초과"
                }
            )
        }
    }

    @Test
    @DisplayName("결제 취소 이벤트 처리 - 재고 복구 및 QR 무효화")
    fun `handlePaymentCancelled should publish restore and invalidation events`() {
        // Given
        val event = PaymentCancelledEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-CANCELLED",
            cancelAmount = 25000,
            cancelReason = "고객 변심",
            customerId = 4L
        )

        // When
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentCancelled(event) }
        }

        // Then - 재고 복구 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<InventoryConfirmationRequestedEvent> { inventoryEvent ->
                    inventoryEvent.actionType == "RESTORE"
                }
            )
        }

        // QR 코드 무효화 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<QrCodeInvalidationRequestedEvent> { qrEvent ->
                    qrEvent.paymentId == paymentId &&
                    qrEvent.orderId == orderId &&
                    qrEvent.reason == "PAYMENT_CANCELLED"
                }
            )
        }
    }

    @Test
    @DisplayName("결제 취소 실패 이벤트 처리 - 재시도 큐 추가")
    fun `handlePaymentCancelFailed should add to retry queue`() {
        // Given
        val event = PaymentCancelFailedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-CANCEL-FAILED",
            cancelReason = "시스템 오류",
            failureReason = "네트워크 타임아웃",
            retryCount = 1,
            customerId = 5L
        )

        // When
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentCancelFailed(event) }
        }

        // Then - 재시도 이벤트 발행 확인 (재시도 횟수가 최대치 미만이므로)
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<PaymentCancelRetryEvent> { retryEvent ->
                    retryEvent.paymentId == paymentId &&
                    retryEvent.orderId == orderId &&
                    retryEvent.retryCount == 2 && // 1 + 1
                    retryEvent.delaySeconds == 10 // 2회차 재시도는 10초
                }
            )
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    @DisplayName("재시도 지연 시간 계산 - 지수 백오프")
    fun `calculateRetryDelay should calculate exponential backoff correctly`(retryCount: Int) {
        // Given & When
        val delaySeconds = invokePrivateMethod(paymentEventListener, "calculateRetryDelay", retryCount) as Int

        // Then
        val expectedDelay = when (retryCount) {
            1 -> 5
            2 -> 10
            3 -> 20
            else -> 30
        }
        assertEquals(expectedDelay, delaySeconds)
    }

    @Test
    @DisplayName("재시도 최대 횟수 초과 시 최종 실패 이벤트 발행")
    fun `handlePaymentCancelFailed should publish final failure event when max retries exceeded`() {
        // Given - 재시도 횟수가 최대치에 도달
        val event = PaymentCancelFailedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-MAX-RETRY",
            cancelReason = "원본 취소 사유",
            failureReason = "최종 실패",
            retryCount = 3, // 최대 재시도 횟수에 도달
            customerId = 6L
        )

        // When
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentCancelFailed(event) }
        }

        // Then - 최종 실패 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<PaymentCancelFinalFailureEvent> { finalFailureEvent ->
                    finalFailureEvent.paymentId == paymentId &&
                    finalFailureEvent.orderId == orderId &&
                    finalFailureEvent.originalReason == "원본 취소 사유" &&
                    finalFailureEvent.finalFailureReason == "최대 재시도 횟수 초과" &&
                    finalFailureEvent.totalRetryCount == 3
                }
            )
        }

        // 재시도 이벤트는 발행되지 않아야 함
        verify(exactly = 0) {
            paymentEventPublisher.publishAsync(
                match<PaymentCancelRetryEvent> { true }
            )
        }
    }

    @Test
    @DisplayName("결제 만료 이벤트 처리 - 재고 복구 및 주문 상태 변경")
    fun `handlePaymentExpired should publish restore and status update events`() {
        // Given
        val event = PaymentExpiredEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-EXPIRED",
            amount = 30000,
            expiredAt = LocalDateTime.now()
        )

        // When
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentExpired(event) }
        }

        // Then - 재고 복구 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<InventoryConfirmationRequestedEvent> { inventoryEvent ->
                    inventoryEvent.actionType == "RESTORE"
                }
            )
        }

        // 주문 만료 상태 변경 이벤트 발행 확인
        verify(exactly = 1) {
            paymentEventPublisher.publishAsync(
                match<OrderStatusUpdateRequestedEvent> { orderEvent ->
                    orderEvent.newStatus == "EXPIRED" &&
                    orderEvent.reason == "결제 만료"
                }
            )
        }
    }

    @Test
    @DisplayName("이벤트 처리 중 예외 발생 시 안전하게 처리")
    fun `event handlers should handle exceptions gracefully`() {
        // Given - PaymentEventPublisher에서 예외 발생
        coEvery { paymentEventPublisher.publishQrCodeGenerationRequested(any(), any(), any(), any()) } throws RuntimeException("QR 생성 실패")
        every { paymentEventPublisher.publishAsync(any()) } throws RuntimeException("이벤트 발행 실패")

        val approvedEvent = PaymentApprovedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-ERROR",
            amount = 5000,
            paymentMethod = "CARD",
            paymentKey = "error_key",
            approvedAt = LocalDateTime.now(),
            customerId = 99L
        )

        // When & Then - 예외가 발생해도 정상적으로 처리되어야 함 (로그만 남기고)
        assertDoesNotThrow {
            runBlocking { paymentEventListener.handlePaymentApproved(approvedEvent) }
        }

        // 메서드 호출은 되었지만 예외로 인해 실패
        coVerify(exactly = 1) {
            paymentEventPublisher.publishQrCodeGenerationRequested(any(), any(), any(), any())
        }
    }

    @Test
    @DisplayName("다양한 결제 금액에 대한 이벤트 처리")
    fun `should handle events with various amounts correctly`() {
        // Given
        val amounts = listOf(100, 1000, 10000, 100000, 1000000)

        amounts.forEach { amount ->
            val event = PaymentCreatedEvent(
                paymentId = UUID.randomUUID(),
                orderId = UUID.randomUUID(),
                orderNo = "ORDER-$amount",
                amount = amount,
                paymentMethod = "CARD",
                customerId = 777L
            )

            // When & Then
            assertDoesNotThrow {
                runBlocking { paymentEventListener.handlePaymentCreated(event) }
            }
        }
    }

    /**
     * 리플렉션을 사용해서 private 메서드 호출
     */
    private fun invokePrivateMethod(instance: Any, methodName: String, vararg args: Any): Any? {
        val method = instance::class.java.getDeclaredMethod(methodName, *args.map { it::class.java }.toTypedArray())
        method.isAccessible = true
        return method.invoke(instance, *args)
    }
}
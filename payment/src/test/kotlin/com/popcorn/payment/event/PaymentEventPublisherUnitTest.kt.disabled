package com.popcorn.payment.event

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("PaymentEventPublisher 단위 테스트")
class PaymentEventPublisherUnitTest {

    private val applicationEventPublisher = mockk<ApplicationEventPublisher>()
    private lateinit var paymentEventPublisher: PaymentEventPublisherImpl

    private val paymentId = UUID.randomUUID()
    private val orderId = UUID.randomUUID()
    private val paymentKey = "test_payment_key_12345"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        paymentEventPublisher = PaymentEventPublisherImpl(applicationEventPublisher)

        // ApplicationEventPublisher의 publishEvent는 void이므로 Unit을 반환하도록 설정
        every { applicationEventPublisher.publishEvent(any()) } just Runs
    }

    @Test
    @DisplayName("단일 이벤트 발행 성공")
    fun `publish should successfully publish single event`() = runTest {
        // Given
        val event = PaymentCreatedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-001",
            amount = 10000,
            paymentMethod = "CARD",
            customerId = 1L
        )

        // When
        paymentEventPublisher.publish(event)

        // Then
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
    }

    @Test
    @DisplayName("다중 이벤트 발행 성공")
    fun `publishAll should successfully publish multiple events`() = runTest {
        // Given
        val events = listOf(
            PaymentCreatedEvent(paymentId, orderId, "ORDER-001", 10000, "CARD", 1L),
            PaymentApprovedEvent(paymentId, orderId, "ORDER-001", 10000, "CARD", paymentKey, LocalDateTime.now(), 1L),
            PaymentCompletedEvent.create(orderId, paymentId, paymentKey, 10000, "CARD")
        )

        // When
        paymentEventPublisher.publishAll(events)

        // Then
        // 각 이벤트가 개별적으로 발행되어야 함
        verify(exactly = events.size) { applicationEventPublisher.publishEvent(any()) }
        events.forEach { event ->
            verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
        }
    }

    @Test
    @DisplayName("결제 생성 이벤트 발행")
    fun `publishPaymentCreated should publish PaymentCreatedEvent correctly`() = runTest {
        // Given
        val orderNo = "ORDER-12345"
        val amount = 25000
        val paymentMethod = "TRANSFER"
        val customerId = 100L

        // When
        paymentEventPublisher.publishPaymentCreated(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = orderNo,
            amount = amount,
            paymentMethod = paymentMethod,
            customerId = customerId
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentCreatedEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.orderNo == orderNo &&
                    event.amount == amount &&
                    event.paymentMethod == paymentMethod &&
                    event.customerId == customerId
                }
            )
        }
    }

    @Test
    @DisplayName("결제 승인 이벤트 발행")
    fun `publishPaymentApproved should publish PaymentApprovedEvent correctly`() = runTest {
        // Given
        val orderNo = "ORDER-67890"
        val amount = 50000
        val paymentMethod = "CARD"
        val approvedAt = LocalDateTime.now()
        val customerId = 200L

        // When
        paymentEventPublisher.publishPaymentApproved(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = orderNo,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentKey = paymentKey,
            approvedAt = approvedAt,
            customerId = customerId
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentApprovedEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.orderNo == orderNo &&
                    event.amount == amount &&
                    event.paymentMethod == paymentMethod &&
                    event.paymentKey == paymentKey &&
                    event.approvedAt == approvedAt &&
                    event.customerId == customerId
                }
            )
        }
    }

    @Test
    @DisplayName("결제 실패 이벤트 발행")
    fun `publishPaymentFailed should publish PaymentFailedEvent correctly`() = runTest {
        // Given
        val orderNo = "ORDER-FAILED"
        val amount = 15000
        val paymentMethod = "VIRTUAL_ACCOUNT"
        val failureReason = "카드 한도 초과"
        val customerId = 300L

        // When
        paymentEventPublisher.publishPaymentFailed(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = orderNo,
            amount = amount,
            paymentMethod = paymentMethod,
            failureReason = failureReason,
            customerId = customerId
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentFailedEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.orderNo == orderNo &&
                    event.amount == amount &&
                    event.paymentMethod == paymentMethod &&
                    event.failureReason == failureReason &&
                    event.customerId == customerId
                }
            )
        }
    }

    @Test
    @DisplayName("결제 취소 이벤트 발행")
    fun `publishPaymentCancelled should publish PaymentCancelledEvent correctly`() = runTest {
        // Given
        val orderNo = "ORDER-CANCELLED"
        val cancelAmount = 30000
        val cancelReason = "고객 요청에 의한 취소"
        val customerId = 400L

        // When
        paymentEventPublisher.publishPaymentCancelled(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = orderNo,
            cancelAmount = cancelAmount,
            cancelReason = cancelReason,
            customerId = customerId
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentCancelledEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.orderNo == orderNo &&
                    event.cancelAmount == cancelAmount &&
                    event.cancelReason == cancelReason &&
                    event.customerId == customerId
                }
            )
        }
    }

    @Test
    @DisplayName("결제 완료 이벤트 발행 - PG 응답 없음")
    fun `publishPaymentCompleted should publish event without PG response`() = runTest {
        // Given
        val amount = 40000
        val paymentMethod = "MOBILE_PHONE"

        // When
        paymentEventPublisher.publishPaymentCompleted(
            paymentId = paymentId,
            orderId = orderId,
            paymentKey = paymentKey,
            amount = amount,
            paymentMethod = paymentMethod,
            pgResponse = null
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentCompletedEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.paymentKey == paymentKey &&
                    event.amount == amount &&
                    event.paymentMethod == paymentMethod &&
                    event.pgResponse == null
                }
            )
        }
    }

    @Test
    @DisplayName("결제 완료 이벤트 발행 - PG 응답 포함")
    fun `publishPaymentCompleted should publish event with PG response`() = runTest {
        // Given
        val amount = 60000
        val paymentMethod = "GIFT_CERTIFICATE"
        val pgResponse = """{"status":"success","transactionId":"TXN123","approvedAt":"2024-01-01T10:00:00"}"""

        // When
        paymentEventPublisher.publishPaymentCompleted(
            paymentId = paymentId,
            orderId = orderId,
            paymentKey = paymentKey,
            amount = amount,
            paymentMethod = paymentMethod,
            pgResponse = pgResponse
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentCompletedEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.paymentKey == paymentKey &&
                    event.amount == amount &&
                    event.paymentMethod == paymentMethod &&
                    event.pgResponse == pgResponse
                }
            )
        }
    }

    @Test
    @DisplayName("QR 코드 생성 요청 이벤트 발행")
    fun `publishQrCodeGenerationRequested should publish event correctly`() = runTest {
        // Given
        val orderNo = "ORDER-QR-001"
        val customerId = 500L

        // When
        paymentEventPublisher.publishQrCodeGenerationRequested(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<QrCodeGenerationRequestedEvent> { event ->
                    event.paymentId == paymentId &&
                    event.orderId == orderId &&
                    event.orderNo == orderNo &&
                    event.customerId == customerId &&
                    event.eventId.isNotBlank()
                }
            )
        }
    }

    @Test
    @DisplayName("비동기 이벤트 발행 - publishAsync")
    fun `publishAsync should publish event asynchronously`() {
        // Given
        val event = PaymentCreatedEvent(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ASYNC-ORDER",
            amount = 8000,
            paymentMethod = "CARD",
            customerId = 600L
        )

        // When
        paymentEventPublisher.publishAsync(event)

        // 비동기 처리가 완료될 시간을 기다림
        Thread.sleep(100)

        // Then
        verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["CARD", "TRANSFER", "VIRTUAL_ACCOUNT", "MOBILE_PHONE", "GIFT_CERTIFICATE"])
    @DisplayName("다양한 결제 수단에 대한 이벤트 발행")
    fun `should publish events for various payment methods`(paymentMethod: String) = runTest {
        // Given
        val amount = 12000

        // When
        paymentEventPublisher.publishPaymentCreated(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-METHOD-TEST",
            amount = amount,
            paymentMethod = paymentMethod,
            customerId = 700L
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentCreatedEvent> { event ->
                    event.paymentMethod == paymentMethod
                }
            )
        }
    }

    @Test
    @DisplayName("이벤트 발행 중 예외 처리 - ApplicationEventPublisher 오류")
    fun `publish should handle ApplicationEventPublisher exception gracefully`() = runTest {
        // Given
        val event = PaymentCreatedEvent(paymentId, orderId, "ORDER-ERROR", 5000, "CARD", 800L)
        every { applicationEventPublisher.publishEvent(any()) } throws RuntimeException("Event publishing failed")

        // When & Then - 예외가 발생하지만 로그만 남기고 정상 처리되어야 함
        assertDoesNotThrow {
            runBlocking { paymentEventPublisher.publish(event) }
        }

        verify(exactly = 1) { applicationEventPublisher.publishEvent(event) }
    }

    @Test
    @DisplayName("빈 이벤트 리스트 발행")
    fun `publishAll should handle empty event list`() = runTest {
        // Given
        val emptyEvents = emptyList<PaymentEvent>()

        // When
        paymentEventPublisher.publishAll(emptyEvents)

        // Then
        verify(exactly = 0) { applicationEventPublisher.publishEvent(any()) }
    }

    @Test
    @DisplayName("null 값이 포함된 이벤트 필드 처리")
    fun `should handle events with nullable fields correctly`() = runTest {
        // Given - customerId가 null인 경우
        // When
        paymentEventPublisher.publishPaymentCreated(
            paymentId = paymentId,
            orderId = orderId,
            orderNo = "ORDER-NULL-CUSTOMER",
            amount = 7000,
            paymentMethod = "CARD",
            customerId = null
        )

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(
                match<PaymentCreatedEvent> { event ->
                    event.customerId == null &&
                    event.paymentId == paymentId &&
                    event.orderId == orderId
                }
            )
        }
    }
}
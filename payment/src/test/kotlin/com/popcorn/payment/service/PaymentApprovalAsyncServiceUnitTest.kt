package com.popcorn.payment.service

import com.popcorn.payment.dto.PaymentConfirmRequest
import com.popcorn.payment.exception.PaymentException
import io.mockk.*
import kotlinx.coroutines.delay
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
@DisplayName("PaymentApprovalAsyncService 단위 테스트")
class PaymentApprovalAsyncServiceUnitTest {

    private val tossPaymentService = mockk<TossPaymentCoroutineService>()
    private lateinit var paymentApprovalAsyncService: PaymentApprovalAsyncService

    private val paymentKey = "test_payment_key_12345"
    private val orderId = "00000000-1234-5678-9abc-000000000001"
    private val amount = 10000

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        paymentApprovalAsyncService = PaymentApprovalAsyncService(tossPaymentService)
    }

    @Test
    @DisplayName("비동기 결제 승인 성공")
    fun `confirmAsync should successfully confirm payment asynchronously`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        val expectedResult = TossPaymentConfirmResult(
            paymentId = UUID.randomUUID(),
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = UUID.fromString(orderId),
            orderNo = "ORDER-001",
            amount = amount,
            approvedAt = LocalDateTime.now()
        )

        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        } returns expectedResult

        // When - 비동기 메서드이므로 직접 호출
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(request)
        }

        // 비동기 처리가 완료될 시간을 기다림
        Thread.sleep(100)

        // Then
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        }
    }

    @Test
    @DisplayName("비동기 결제 승인 - PaymentException 발생 시 예외 처리")
    fun `confirmAsync should handle PaymentException gracefully`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        val errorMessage = "결제 승인 실패"
        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        } throws PaymentException.invalidRequest(errorMessage)

        // When - 예외가 발생해도 정상적으로 처리되어야 함 (로그만 기록)
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(request)
        }

        // 비동기 처리 완료 대기
        Thread.sleep(100)

        // Then
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        }
    }

    @Test
    @DisplayName("비동기 결제 승인 - 일반 예외 발생 시 예외 처리")
    fun `confirmAsync should handle general exception gracefully`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        val errorMessage = "시스템 오류"
        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        } throws RuntimeException(errorMessage)

        // When - 예외가 발생해도 메서드는 정상 완료되어야 함
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(request)
        }

        // 비동기 처리 완료 대기
        Thread.sleep(100)

        // Then
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1000, 5000, 10000, 50000, 100000])
    @DisplayName("다양한 금액에 대한 비동기 결제 승인")
    fun `confirmAsync should handle various amounts`(testAmount: Int) {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = testAmount
        )

        val expectedResult = TossPaymentConfirmResult(
            paymentId = UUID.randomUUID(),
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = UUID.fromString(orderId),
            orderNo = "ORDER-${testAmount}",
            amount = testAmount,
            approvedAt = LocalDateTime.now()
        )

        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, testAmount)
        } returns expectedResult

        // When
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(request)
        }

        Thread.sleep(50)

        // Then
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(paymentKey, orderId, testAmount)
        }
    }

    @Test
    @DisplayName("비동기 실행 검증 - 메서드 즉시 반환")
    fun `confirmAsync should return immediately due to async execution`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        // TossPaymentService가 지연되도록 설정
        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        } coAnswers {
            delay(1000) // 1초 지연
            TossPaymentConfirmResult(
                paymentId = UUID.randomUUID(),
                paymentStatus = "PAID",
                orderStatus = "COMPLETED",
                orderId = UUID.fromString(orderId),
                orderNo = "ORDER-DELAY",
                amount = amount,
                approvedAt = LocalDateTime.now()
            )
        }

        // When - 비동기 메서드는 즉시 반환되어야 함
        val startTime = System.currentTimeMillis()
        paymentApprovalAsyncService.confirmAsync(request)
        val endTime = System.currentTimeMillis()

        // Then - 메서드 실행이 즉시 완료되어야 함 (1초 이내)
        val executionTime = endTime - startTime
        assertTrue(executionTime < 500, "비동기 메서드는 즉시 반환되어야 함. 실행 시간: ${executionTime}ms")
    }

    @Test
    @DisplayName("runBlocking 컨텍스트 검증 - suspend 함수 호출")
    fun `confirmAsync should properly handle runBlocking context`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        // suspend 함수가 정상적으로 호출되는지 확인
        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        } returns TossPaymentConfirmResult(
            paymentId = UUID.randomUUID(),
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = UUID.fromString(orderId),
            orderNo = "ORDER-RUNBLOCKING",
            amount = amount,
            approvedAt = LocalDateTime.now()
        )

        // When
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(request)
        }

        Thread.sleep(100)

        // Then - suspend 함수가 runBlocking 컨텍스트에서 정상 호출되어야 함
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        }
    }

    @Test
    @DisplayName("요청 매개변수 검증")
    fun `confirmAsync should properly pass request parameters`() {
        // Given
        val customPaymentKey = "custom_payment_key_999"
        val customOrderId = "99999999-0000-0000-0000-000000000999"
        val customAmount = 50000

        val request = PaymentConfirmRequest(
            paymentKey = customPaymentKey,
            orderId = customOrderId,
            amount = customAmount
        )

        coEvery {
            tossPaymentService.confirmPayment(customPaymentKey, customOrderId, customAmount)
        } returns TossPaymentConfirmResult(
            paymentId = UUID.randomUUID(),
            paymentStatus = "PAID",
            orderStatus = "COMPLETED",
            orderId = UUID.fromString(customOrderId),
            orderNo = "CUSTOM-ORDER",
            amount = customAmount,
            approvedAt = LocalDateTime.now()
        )

        // When
        paymentApprovalAsyncService.confirmAsync(request)
        Thread.sleep(100)

        // Then - 정확한 매개변수로 호출되어야 함
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(customPaymentKey, customOrderId, customAmount)
        }
    }

    @Test
    @DisplayName("다중 비동기 호출 처리")
    fun `confirmAsync should handle multiple concurrent calls`() {
        // Given
        val requests = listOf(
            PaymentConfirmRequest("key1", "order1", 1000),
            PaymentConfirmRequest("key2", "order2", 2000),
            PaymentConfirmRequest("key3", "order3", 3000)
        )

        // 각 호출에 대한 응답 설정
        requests.forEach { request ->
            coEvery {
                tossPaymentService.confirmPayment(request.paymentKey, request.orderId, request.amount)
            } returns TossPaymentConfirmResult(
                paymentId = UUID.randomUUID(),
                paymentStatus = "PAID",
                orderStatus = "COMPLETED",
                orderId = UUID.randomUUID(),
                orderNo = "ORDER-${request.amount}",
                amount = request.amount,
                approvedAt = LocalDateTime.now()
            )
        }

        // When - 동시에 여러 요청 처리
        requests.forEach { request ->
            assertDoesNotThrow {
                paymentApprovalAsyncService.confirmAsync(request)
            }
        }

        Thread.sleep(200)

        // Then - 모든 호출이 처리되어야 함
        requests.forEach { request ->
            coVerify(exactly = 1) {
                tossPaymentService.confirmPayment(request.paymentKey, request.orderId, request.amount)
            }
        }
    }

    @Test
    @DisplayName("예외 발생 시 로깅 동작 확인")
    fun `confirmAsync should log error when exception occurs`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount
        )

        // 특정 예외 발생 시뮬레이션
        val specificError = PaymentException.amountMismatch("금액 불일치 오류")
        coEvery {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        } throws specificError

        // When - 예외 발생해도 메서드는 정상 완료
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(request)
        }

        Thread.sleep(100)

        // Then
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment(paymentKey, orderId, amount)
        }
        // 실제 로깅 검증은 통합 테스트나 로깅 프레임워크 모킹이 필요
    }

    @Test
    @DisplayName("빈 요청 객체 처리")
    fun `confirmAsync should handle request with minimum fields`() {
        // Given
        val minimalRequest = PaymentConfirmRequest(
            paymentKey = "",
            orderId = "",
            amount = 0
        )

        // 서비스에서 빈 값에 대한 예외 발생 예상
        coEvery {
            tossPaymentService.confirmPayment("", "", 0)
        } throws PaymentException.invalidRequest("잘못된 요청 데이터")

        // When - 예외가 발생해도 정상 처리되어야 함
        assertDoesNotThrow {
            paymentApprovalAsyncService.confirmAsync(minimalRequest)
        }

        Thread.sleep(100)

        // Then
        coVerify(exactly = 1) {
            tossPaymentService.confirmPayment("", "", 0)
        }
    }
}

// 테스트용 Mock DTO (TossPaymentConfirmResult는 이미 TossPaymentCoroutineService 테스트에서 정의됨)
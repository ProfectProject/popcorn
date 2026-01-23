package com.popcorn.payment.service

import com.popcorn.payment.entity.Payment
import com.popcorn.payment.entity.PaymentMethod
import com.popcorn.payment.entity.PaymentStatus
import com.popcorn.payment.exception.PaymentException
import com.popcorn.payment.repository.PaymentRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("PaymentCommandCoroutineService 단위 테스트")
class PaymentCommandCoroutineServiceUnitTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private lateinit var paymentCommandService: PaymentCommandCoroutineService

    private val orderId = UUID.randomUUID()
    private val paymentId = UUID.randomUUID()
    private val paymentKey = "test_payment_key_12345"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        paymentCommandService = PaymentCommandCoroutineService(paymentRepository)
    }

    @Test
    @DisplayName("결제 생성 성공 - 모든 필드가 올바르게 설정됨")
    fun `createPayment should successfully create payment with all fields`() = runBlocking {
        // Given
        val amount = 10000
        val paymentMethod = "CARD"
        val rawPayload = """{"orderId":"$orderId","amount":$amount}"""
        val customerId = 1L

        val expectedPayment = Payment.create(
            orderId = orderId,
            amount = amount,
            paymentMethod = PaymentMethod.CARD,
            paymentKey = paymentKey,
            customerId = customerId,
            rawPayload = rawPayload
        ).copy(paymentId = paymentId)

        every { paymentRepository.save(any()) } returns expectedPayment

        // When
        val result = paymentCommandService.createPayment(
            orderId = orderId,
            paymentMethod = paymentMethod,
            amount = amount,
            paymentKey = paymentKey,
            customerId = customerId,
            rawPayload = rawPayload
        )

        // Then
        assertNotNull(result)
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(amount, result.amount)
        assertEquals(PaymentMethod.CARD, result.paymentMethod)
        assertEquals(PaymentStatus.READY, result.status)
        assertEquals(paymentKey, result.paymentKey)
        assertEquals(customerId, result.customerId)
        assertEquals(rawPayload, result.rawPayload)

        verify(exactly = 1) { paymentRepository.save(any()) }
    }

    @Test
    @DisplayName("결제 생성 블로킹 - 동기 처리 확인")
    fun `createPaymentBlocking should create payment synchronously`() {
        // Given
        val amount = 15000
        val paymentMethod = "TRANSFER"
        val rawPayload = """{"orderId":"$orderId","method":"transfer"}"""

        val expectedPayment = Payment.create(
            orderId = orderId,
            amount = amount,
            paymentMethod = PaymentMethod.TRANSFER,
            paymentKey = paymentKey,
            rawPayload = rawPayload
        ).copy(paymentId = paymentId)

        every { paymentRepository.save(any()) } returns expectedPayment

        // When
        val result = paymentCommandService.createPaymentBlocking(
            orderId = orderId,
            paymentMethod = paymentMethod,
            amount = amount,
            paymentKey = paymentKey,
            rawPayload = rawPayload
        )

        // Then
        assertNotNull(result)
        assertEquals(paymentId, result.paymentId)
        assertEquals(PaymentMethod.TRANSFER, result.paymentMethod)
        verify(exactly = 1) { paymentRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus::class)
    @DisplayName("결제 상태 업데이트 - 모든 상태 전환 테스트")
    fun `updatePaymentStatus should update all payment statuses correctly`(status: PaymentStatus) = runBlocking {
        // Given
        val originalPayment = Payment.create(
            orderId = orderId,
            amount = 20000,
            paymentMethod = PaymentMethod.CARD,
            paymentKey = paymentKey
        ).copy(paymentId = paymentId)

        val updatedPayment = originalPayment.copy(status = status, approvedAt = LocalDateTime.now())

        every { paymentRepository.findById(paymentId) } returns Optional.of(originalPayment)
        every { paymentRepository.save(any()) } returns updatedPayment

        // When
        val result = paymentCommandService.updatePaymentStatus(
            paymentId = paymentId,
            status = status.name,
            approvedAt = LocalDateTime.now(),
            rawPayload = """{"status":"${status.name}"}"""
        )

        // Then
        assertNotNull(result)
        assertEquals(status, result.status)
        assertEquals(paymentId, result.paymentId)

        verify(exactly = 1) { paymentRepository.findById(paymentId) }
        verify(exactly = 1) { paymentRepository.save(any()) }
    }

    @Test
    @DisplayName("결제 상태 업데이트 실패 - 존재하지 않는 결제 ID")
    fun `updatePaymentStatus should throw exception for non-existent payment`() = runBlocking {
        // Given
        val nonExistentPaymentId = UUID.randomUUID()
        every { paymentRepository.findById(nonExistentPaymentId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(PaymentException.PaymentNotFound::class.java) {
            runBlocking {
                paymentCommandService.updatePaymentStatus(
                    paymentId = nonExistentPaymentId,
                    status = "PAID"
                )
            }
        }

        assertTrue(exception.message!!.contains("결제 정보를 찾을 수 없습니다"))
        verify(exactly = 1) { paymentRepository.findById(nonExistentPaymentId) }
        verify(exactly = 0) { paymentRepository.save(any()) }
    }

    @Test
    @DisplayName("PaymentKey로 결제 조회 성공")
    fun `findByPaymentKey should return payments successfully`() = runBlocking {
        // Given
        val payment1 = Payment.create(orderId, 10000, PaymentMethod.CARD, paymentKey).copy(paymentId = UUID.randomUUID())
        val payment2 = Payment.create(orderId, 5000, PaymentMethod.CARD, paymentKey).copy(paymentId = UUID.randomUUID())
        val expectedPayments = listOf(payment1, payment2)

        every { paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(paymentKey) } returns expectedPayments

        // When
        val result = paymentCommandService.findByPaymentKey(paymentKey)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(payment1.paymentId, result[0].paymentId)
        assertEquals(payment2.paymentId, result[1].paymentId)
        assertTrue(result.all { it.paymentKey == paymentKey })

        verify(exactly = 1) { paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(paymentKey) }
    }

    @Test
    @DisplayName("PaymentKey로 결제 조회 - 빈 결과")
    fun `findByPaymentKey should return empty list when no payments found`() = runBlocking {
        // Given
        val unknownPaymentKey = "unknown_payment_key"
        every { paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(unknownPaymentKey) } returns emptyList()

        // When
        val result = paymentCommandService.findByPaymentKey(unknownPaymentKey)

        // Then
        assertNotNull(result)
        assertTrue(result.isEmpty())

        verify(exactly = 1) { paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(unknownPaymentKey) }
    }

    @Test
    @DisplayName("주문 ID로 최신 결제 조회 성공")
    fun `getLatestPaymentByOrderId should return latest payment`() = runBlocking {
        // Given
        val latestPayment = Payment.create(
            orderId = orderId,
            amount = 25000,
            paymentMethod = PaymentMethod.CARD,
            paymentKey = paymentKey
        ).copy(paymentId = paymentId)

        every { paymentRepository.findFirstByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId) } returns latestPayment

        // When
        val result = paymentCommandService.getLatestPaymentByOrderId(orderId)

        // Then
        assertNotNull(result)
        assertEquals(latestPayment.paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals(25000, result.amount)

        verify(exactly = 1) { paymentRepository.findFirstByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId) }
    }

    @Test
    @DisplayName("주문 ID로 최신 결제 조회 실패 - 결제 없음")
    fun `getLatestPaymentByOrderId should throw exception when no payment found`() {
        // Given
        val unknownOrderId = UUID.randomUUID()
        every { paymentRepository.findFirstByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(unknownOrderId) } returns null

        // When & Then
        val exception = assertThrows(PaymentException.PaymentNotFound::class.java) {
            runBlocking {
                paymentCommandService.getLatestPaymentByOrderId(unknownOrderId)
            }
        }

        assertTrue(exception.message!!.contains("주문에 대한 결제 정보를 찾을 수 없습니다"))
        verify(exactly = 1) { paymentRepository.findFirstByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(unknownOrderId) }
    }

    @Test
    @DisplayName("결제 생성 입력값 검증 - 금액이 0 이하")
    fun `validatePaymentCreation should throw exception for invalid amount`() {
        // Given
        val invalidAmounts = listOf(0, -100, -1)

        invalidAmounts.forEach { invalidAmount ->
            // When & Then
            val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
                paymentCommandService.validatePaymentCreation(
                    orderId = orderId,
                    amount = invalidAmount,
                    paymentMethod = "CARD",
                    paymentKey = paymentKey
                )
            }
            assertTrue(exception.message!!.contains("결제 금액은 0보다 커야 합니다"))
        }
    }

    @Test
    @DisplayName("결제 생성 입력값 검증 - 빈 PaymentKey")
    fun `validatePaymentCreation should throw exception for blank paymentKey`() {
        // Given
        val invalidPaymentKeys = listOf("", "   ", "\t", "\n")

        invalidPaymentKeys.forEach { invalidKey ->
            // When & Then
            val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
                paymentCommandService.validatePaymentCreation(
                    orderId = orderId,
                    amount = 10000,
                    paymentMethod = "CARD",
                    paymentKey = invalidKey
                )
            }
            assertTrue(exception.message!!.contains("PaymentKey는 필수입니다"))
        }
    }

    @Test
    @DisplayName("결제 생성 입력값 검증 - 유효하지 않은 결제 수단")
    fun `validatePaymentCreation should throw exception for invalid payment method`() {
        // Given
        val invalidPaymentMethods = listOf("INVALID", "CRYPTO", "CASH", "", "  ")

        invalidPaymentMethods.forEach { invalidMethod ->
            // When & Then
            val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
                paymentCommandService.validatePaymentCreation(
                    orderId = orderId,
                    amount = 10000,
                    paymentMethod = invalidMethod,
                    paymentKey = paymentKey
                )
            }
            assertTrue(exception.message!!.contains("지원하지 않는 결제 수단입니다"))
        }
    }

    @Test
    @DisplayName("결제 생성 입력값 검증 성공 - 모든 유효한 결제 수단")
    fun `validatePaymentCreation should succeed for all valid payment methods`() {
        // Given
        val validPaymentMethods = listOf("CARD", "TRANSFER", "VIRTUAL_ACCOUNT", "MOBILE_PHONE", "GIFT_CERTIFICATE")

        validPaymentMethods.forEach { validMethod ->
            // When & Then - 예외 발생하지 않아야 함
            assertDoesNotThrow {
                paymentCommandService.validatePaymentCreation(
                    orderId = orderId,
                    amount = 10000,
                    paymentMethod = validMethod,
                    paymentKey = paymentKey
                )
            }
        }
    }

    @Test
    @DisplayName("결제 금액 상한선 검증 - 최대 500만원")
    fun `validatePaymentCreation should throw exception for amount exceeding limit`() {
        // Given
        val exceedingAmounts = listOf(5_000_001, 10_000_000, 1_000_000_000)

        exceedingAmounts.forEach { exceedingAmount ->
            // When & Then
            val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
                paymentCommandService.validatePaymentCreation(
                    orderId = orderId,
                    amount = exceedingAmount,
                    paymentMethod = "CARD",
                    paymentKey = paymentKey
                )
            }
            assertTrue(exception.message!!.contains("결제 금액이 한도를 초과했습니다"))
        }
    }

    @Test
    @DisplayName("결제 금액 정상 범위 검증 성공")
    fun `validatePaymentCreation should succeed for valid amounts`() {
        // Given
        val validAmounts = listOf(1, 100, 10000, 100000, 5_000_000)

        validAmounts.forEach { validAmount ->
            // When & Then - 예외 발생하지 않아야 함
            assertDoesNotThrow {
                paymentCommandService.validatePaymentCreation(
                    orderId = orderId,
                    amount = validAmount,
                    paymentMethod = "CARD",
                    paymentKey = paymentKey
                )
            }
        }
    }
}
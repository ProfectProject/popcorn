package com.popcorn.payment.service

import com.popcorn.payment.config.CoroutineTransactionManager
import com.popcorn.payment.entity.Payment
import com.popcorn.payment.entity.PaymentMethod
import com.popcorn.payment.entity.PaymentStatus
import com.popcorn.payment.exception.PaymentException
import com.popcorn.payment.repository.PaymentRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PaymentCommandCoroutineService 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 결제 생성, 상태 업데이트, 조회 로직 테스트
 * - 트랜잭션 관리 및 예외 처리 테스트
 * - 입력 값 검증 및 비즈니스 로직 테스트
 * - 멱등성 키 중복 처리 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentCommandCoroutineServiceTest {

    private val transactionManager = mockk<CoroutineTransactionManager>()
    private val paymentRepository = mockk<PaymentRepository>()
    private val service = PaymentCommandCoroutineService(transactionManager, paymentRepository)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `블로킹 메서드 직접 테스트 - 정상 케이스`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = 15000
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        val mockPayment = createMockPayment(orderId, PaymentMethod.CARD, amount, paymentKey)
        every { paymentRepository.save(any<Payment>()) } returns mockPayment

        // When
        val result = service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)

        // Then
        assertEquals(mockPayment.id, result.paymentId)
        assertEquals("READY", result.status)
        assertEquals(amount, result.amount)
        assertNotNull(result.createdAt)

        verify { paymentRepository.save(any<Payment>()) }
    }

    @Test
    fun `블로킹 메서드 직접 테스트 - 유효하지 않은 금액`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = -1000
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    @Test
    fun `블로킹 메서드 직접 테스트 - 유효하지 않은 결제 수단`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "INVALID_METHOD"
        val amount = 15000
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    @Test
    fun `블로킹 메서드 직접 테스트 - 멱등성 키 중복 처리`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = 15000
        val paymentKey = "duplicate_key"
        val rawPayload = "{\"test\": \"data\"}"

        val existingPayment = createMockPayment(orderId, PaymentMethod.CARD, amount, paymentKey)

        every { paymentRepository.save(any<Payment>()) } throws DataIntegrityViolationException("Duplicate key")
        every {
            paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(paymentKey)
        } returns listOf(existingPayment)

        // When
        val result = service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)

        // Then
        assertEquals(existingPayment.id, result.paymentId)
        assertEquals("READY", result.status)

        verify { paymentRepository.save(any<Payment>()) }
        verify { paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(paymentKey) }
    }

    @Test
    fun `결제 상태 업데이트 블로킹 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val newStatus = "PAID"
        val approvedAt = LocalDateTime.now()
        val rawPayload = "{\"status\": \"approved\"}"

        val mockPayment = createMockPayment(orderId, PaymentMethod.CARD, 15000, "test_key")
        every { mockPayment.id } returns paymentId
        every { mockPayment.status } returns PaymentStatus.PAID

        every { paymentRepository.findById(paymentId) } returns Optional.of(mockPayment)
        every { paymentRepository.save(mockPayment) } returns mockPayment
        every { mockPayment.updateStatus(PaymentStatus.PAID, any()) } just Runs
        every { mockPayment.rawPayload = rawPayload } just Runs

        // When
        val result = service.updatePaymentStatusBlocking(paymentId, newStatus, approvedAt, rawPayload)

        // Then
        assertEquals(paymentId, result.paymentId)
        assertEquals(orderId, result.orderId)
        assertEquals("PAID", result.status)

        verify { paymentRepository.findById(paymentId) }
        verify { mockPayment.updateStatus(PaymentStatus.PAID, any()) }
        verify { paymentRepository.save(mockPayment) }
    }

    @Test
    fun `결제 상태 업데이트 시 결제 정보 없음 예외 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val newStatus = "PAID"

        every { paymentRepository.findById(paymentId) } returns Optional.empty()

        // When & Then
        assertThrows<PaymentException.PaymentNotFound> {
            service.updatePaymentStatusBlocking(paymentId, newStatus)
        }

        verify { paymentRepository.findById(paymentId) }
    }

    @Test
    fun `PaymentCreationResult 데이터 클래스 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val status = "READY"
        val amount = 30000
        val createdAt = LocalDateTime.now()

        // When
        val result1 = PaymentCreationResult(paymentId, status, amount, createdAt)
        val result2 = PaymentCreationResult(paymentId, status, amount, createdAt)
        val result3 = PaymentCreationResult(UUID.randomUUID(), status, amount, createdAt)

        // Then
        assertEquals(paymentId, result1.paymentId)
        assertEquals(status, result1.status)
        assertEquals(amount, result1.amount)
        assertEquals(createdAt, result1.createdAt)

        // equals 및 hashCode 테스트
        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
        assertTrue(result1 != result3)

        // toString 테스트
        assertTrue(result1.toString().contains(paymentId.toString()))
        assertTrue(result1.toString().contains(status))
        assertTrue(result1.toString().contains(amount.toString()))

        // copy 테스트
        val copied = result1.copy(status = "PAID", amount = 50000)
        assertEquals(paymentId, copied.paymentId)
        assertEquals("PAID", copied.status)
        assertEquals(50000, copied.amount)
        assertEquals(createdAt, copied.createdAt)
    }

    @Test
    fun `PaymentDetailResult 데이터 클래스 테스트`() {
        // Given
        val paymentId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentKey = "test_key"
        val status = "PAID"
        val amount = 25000
        val approvedAt = LocalDateTime.now()
        val rawPayload = "{\"approved\": true}"

        // When
        val result1 = PaymentDetailResult(
            paymentId = paymentId,
            orderId = orderId,
            paymentKey = paymentKey,
            status = status,
            amount = amount,
            approvedAt = approvedAt,
            rawPayload = rawPayload
        )

        val result2 = PaymentDetailResult(
            paymentId = paymentId,
            status = status,
            amount = amount,
            approvedAt = null,
            rawPayload = null
        )

        // Then
        assertEquals(paymentId, result1.paymentId)
        assertEquals(orderId, result1.orderId)
        assertEquals(paymentKey, result1.paymentKey)
        assertEquals(status, result1.status)
        assertEquals(amount, result1.amount)
        assertEquals(approvedAt, result1.approvedAt)
        assertEquals(rawPayload, result1.rawPayload)

        // 기본값 테스트
        assertNull(result2.orderId)
        assertNull(result2.paymentKey)
        assertNull(result2.approvedAt)
        assertNull(result2.rawPayload)

        // copy 테스트
        val copied = result1.copy(status = "CANCELLED", amount = 0)
        assertEquals("CANCELLED", copied.status)
        assertEquals(0, copied.amount)
        assertEquals(paymentId, copied.paymentId)
        assertEquals(orderId, copied.orderId)
    }

    @Test
    fun `유효한 결제 수단 검증 테스트`() {
        // Given
        val validMethods = listOf("CARD", "TRANSFER", "VIRTUAL_ACCOUNT", "MOBILE_PHONE")

        validMethods.forEach { method ->
            // Given
            val orderId = UUID.randomUUID()
            val amount = 15000
            val paymentKey = "test_key_$method"
            val rawPayload = "{\"test\": \"data\"}"

            val mockPayment = createMockPayment(orderId, PaymentMethod.valueOf(method), amount, paymentKey)
            every { paymentRepository.save(any<Payment>()) } returns mockPayment

            // When
            val result = service.createPaymentBlocking(orderId, method, amount, paymentKey, rawPayload)

            // Then
            assertEquals("READY", result.status)
            assertEquals(amount, result.amount)
            assertNotNull(result.paymentId)
            assertNotNull(result.createdAt)
        }
    }

    @Test
    fun `예외 케이스 테스트 - 빈 결제 수단`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = ""
        val amount = 15000
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    @Test
    fun `금액 0원 예외 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = 0
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    @Test
    fun `공백 결제 수단 예외 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "   "
        val amount = 15000
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    @Test
    fun `지원하지 않는 결제 수단 예외 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "BITCOIN"
        val amount = 15000
        val paymentKey = "test_key"
        val rawPayload = "{\"test\": \"data\"}"

        // When & Then
        assertThrows<PaymentException.InvalidRequest> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    @Test
    fun `멱등성 키가 없는 경우 예외 재던지기 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = 15000
        val rawPayload = "{\"test\": \"data\"}"

        every { paymentRepository.save(any<Payment>()) } throws DataIntegrityViolationException("Constraint violation")

        // When & Then
        assertThrows<DataIntegrityViolationException> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, null, rawPayload)
        }
    }

    @Test
    fun `멱등성 키 중복 시 기존 결제를 찾을 수 없는 경우 예외 재던지기 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val paymentMethod = "CARD"
        val amount = 15000
        val paymentKey = "missing_key"
        val rawPayload = "{\"test\": \"data\"}"

        every { paymentRepository.save(any<Payment>()) } throws DataIntegrityViolationException("Duplicate key")
        every {
            paymentRepository.findByPaymentKeyAndDeletedAtIsNullOrderByCreatedAtDesc(paymentKey)
        } returns emptyList()

        // When & Then
        assertThrows<DataIntegrityViolationException> {
            service.createPaymentBlocking(orderId, paymentMethod, amount, paymentKey, rawPayload)
        }
    }

    private fun createMockPayment(
        orderId: UUID,
        paymentMethod: PaymentMethod,
        amount: Int,
        paymentKey: String
    ): Payment {
        val payment = mockk<Payment>()
        every { payment.id } returns UUID.randomUUID()
        every { payment.orderId } returns orderId
        every { payment.paymentMethod } returns paymentMethod
        every { payment.amount } returns amount
        every { payment.status } returns PaymentStatus.READY
        every { payment.paymentKey } returns paymentKey
        every { payment.approvedAt } returns null
        every { payment.rawPayload } returns "{\"mock\": \"data\"}"
        every { payment.createdAt } returns LocalDateTime.now()
        every { payment.rawPayload = any() } just Runs
        return payment
    }
}
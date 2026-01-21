package com.popcorn.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.popcorn.payment.client.TossPaymentsCoroutineClient
import com.popcorn.payment.config.CoroutineTransactionManager
import com.popcorn.payment.dto.TossPaymentConfirmResponse
import com.popcorn.payment.exception.PaymentException
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class TossPaymentCoroutineServiceTest {

    private val tossClient = mockk<TossPaymentsCoroutineClient>()
    private val transactionManager = mockk<CoroutineTransactionManager>()
    private val paymentCommandService = mockk<PaymentCommandCoroutineService>()
    private val orderQueryService = mockk<OrderQueryCoroutineService>()
    private val objectMapper = ObjectMapper()

    private val service = TossPaymentCoroutineService(
        tossClient,
        transactionManager,
        paymentCommandService,
        orderQueryService,
        objectMapper
    )

    @Test
    fun `결제 승인 성공 테스트`() = runBlocking {
        // Given
        val paymentKey = "test_payment_key"
        val orderId = UUID.randomUUID().toString()
        val amount = 10000

        val mockOrder = OrderInfo(
            id = UUID.fromString(orderId),
            orderNo = "ORDER-12345",
            customerId = 1L,
            totalAmount = amount,
            status = "PENDING",
            orderType = "PURCHASE",
            createdAt = LocalDateTime.now()
        )

        val mockTossResponse = TossPaymentConfirmResponse(
            paymentKey = paymentKey,
            orderId = orderId,
            totalAmount = amount,
            status = "DONE",
            method = "CARD",
            approvedAt = LocalDateTime.now().toString()
        )

        val mockPaymentResult = PaymentCreationResult(
            paymentId = UUID.randomUUID(),
            status = "READY",
            amount = amount,
            createdAt = LocalDateTime.now()
        )

        // Mock 설정
        coEvery { orderQueryService.getOrder(any()) } returns mockOrder
        coEvery { paymentCommandService.findByPaymentKey(any()) } returns emptyList()
        coEvery { tossClient.confirm(any()) } returns mockTossResponse
        coEvery { paymentCommandService.createPaymentBlocking(any(), any(), any(), any(), any()) } returns mockPaymentResult
        coEvery { paymentCommandService.updatePaymentStatusBlocking(any(), any(), any(), any()) } returns mockk()
        coEvery { orderQueryService.updateOrderStatus(any(), any(), any()) } returns mockOrder.copy(status = "PAID")
        coEvery { transactionManager.executeInTransactionSuspend<Any>(any()) } answers {
            val block = firstArg<suspend () -> Any>()
            runBlocking { block() }
        }

        // When
        val result = service.confirmPayment(paymentKey, orderId, amount)

        // Then
        assertEquals("PAID", result.paymentStatus)
        assertEquals(amount, result.amount)
        coVerify { tossClient.confirm(any()) }
        coVerify { paymentCommandService.createPaymentBlocking(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `결제 승인 실패 - 금액 불일치 테스트`() = runBlocking {
        // Given
        val paymentKey = "test_payment_key"
        val orderId = UUID.randomUUID().toString()
        val requestAmount = 10000
        val tossAmount = 15000

        val mockOrder = OrderInfo(
            id = UUID.fromString(orderId),
            orderNo = "ORDER-12345",
            customerId = 1L,
            totalAmount = requestAmount,
            status = "PENDING",
            orderType = "PURCHASE",
            createdAt = LocalDateTime.now()
        )

        val mockTossResponse = TossPaymentConfirmResponse(
            paymentKey = paymentKey,
            orderId = orderId,
            totalAmount = tossAmount, // 다른 금액
            status = "DONE",
            method = "CARD",
            approvedAt = LocalDateTime.now().toString()
        )

        coEvery { orderQueryService.getOrder(any()) } returns mockOrder
        coEvery { paymentCommandService.findByPaymentKey(any()) } returns emptyList()
        coEvery { tossClient.confirm(any()) } returns mockTossResponse

        // When & Then
        assertThrows<PaymentException.AmountMismatch> {
            service.confirmPayment(paymentKey, orderId, requestAmount)
        }
    }

    @Test
    fun `멱등성 체크 테스트`() = runBlocking {
        // Given
        val paymentKey = "test_payment_key"
        val orderId = UUID.randomUUID().toString()
        val amount = 10000

        val existingPayment = PaymentDetailResult(
            paymentId = UUID.randomUUID(),
            orderId = UUID.fromString(orderId),
            status = "PAID",
            amount = amount,
            approvedAt = LocalDateTime.now(),
            rawPayload = null
        )

        val mockOrder = OrderInfo(
            id = UUID.fromString(orderId),
            orderNo = "ORDER-12345",
            customerId = 1L,
            totalAmount = amount,
            status = "PAID",
            orderType = "PURCHASE",
            createdAt = LocalDateTime.now()
        )

        coEvery { paymentCommandService.findByPaymentKey(paymentKey) } returns listOf(existingPayment)
        coEvery { orderQueryService.getOrder(any()) } returns mockOrder

        // When
        val result = service.confirmPayment(paymentKey, orderId, amount)

        // Then
        assertEquals("PAID", result.paymentStatus)
        assertEquals(amount, result.amount)

        // 토스 API는 호출되지 않아야 함
        coVerify(exactly = 0) { tossClient.confirm(any()) }
    }
}

package com.popcorn.payment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Disabled
import com.popcorn.payment.dto.PaymentConfirmRequest
import com.popcorn.payment.dto.PaymentCreateRequest
import com.popcorn.payment.service.TossPaymentConfirmResult
import com.popcorn.payment.service.TossPaymentCoroutineService
import com.popcorn.payment.service.PaymentCommandCoroutineService
import com.popcorn.payment.service.OrderQueryCoroutineService
import com.popcorn.payment.util.PaymentTokenUtil
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(PaymentController::class)
class PaymentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun tossPaymentService(): TossPaymentCoroutineService = mockk()

        @Bean
        @Primary
        fun paymentCommandService(): PaymentCommandCoroutineService = mockk()

        @Bean
        @Primary
        fun paymentTokenUtil(): PaymentTokenUtil = mockk()

        @Bean
        @Primary
        fun orderQueryService(): OrderQueryCoroutineService = mockk()

        @Bean
        @Primary
        fun paymentApprovalAsyncService(): com.popcorn.payment.service.PaymentApprovalAsyncService = mockk()
    }

    @Autowired
    private lateinit var tossPaymentService: TossPaymentCoroutineService

    @Autowired
    private lateinit var paymentCommandService: PaymentCommandCoroutineService

    @Autowired
    private lateinit var paymentTokenUtil: PaymentTokenUtil

    @Test
    fun `결제 승인 성공 테스트`() {
        // Given
        val request = PaymentConfirmRequest(
            paymentKey = "test_payment_key",
            orderId = "test_order_id",
            amount = 10000
        )

        val mockResult = TossPaymentConfirmResult(
            paymentId = UUID.randomUUID(),
            paymentStatus = "PAID",
            orderStatus = "PAID",
            orderId = UUID.randomUUID(),
            orderNo = "ORDER-12345",
            amount = 10000,
            approvedAt = LocalDateTime.now()
        )

        coEvery {
            tossPaymentService.confirmPayment(any(), any(), any())
        } returns mockResult

        // When & Then
        runBlocking {
            mockMvc.post("/api/pay/v1/payments/confirm") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.paymentStatus") { value("PAID") }
                jsonPath("$.data.amount") { value(10000) }
            }
        }
    }

    @Test
    fun `결제 생성 성공 테스트`() {
        // Given
        val request = PaymentCreateRequest(
            orderId = UUID.randomUUID(),
            paymentMethod = "CARD",
            amount = 15000
        )
        every { paymentTokenUtil.generatePaymentToken(any(), any(), any(), any()) } returns "test_token"

        // When & Then
        runBlocking {
            mockMvc.post("/api/pay/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.status") { value("READY") }
                jsonPath("$.data.amount") { value(15000) }
                jsonPath("$.data.paymentUrl") { value("http://localhost:3000/payment?token=test_token") }
            }
        }
    }

    @Test
    fun `유효하지 않은 요청 테스트`() {
        // Given - 빈 paymentKey
        val invalidRequest = PaymentConfirmRequest(
            paymentKey = "",
            orderId = "test_order_id",
            amount = 10000
        )

        // When & Then
        runBlocking {
            mockMvc.post("/api/pay/v1/payments/confirm") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(invalidRequest)
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }
}

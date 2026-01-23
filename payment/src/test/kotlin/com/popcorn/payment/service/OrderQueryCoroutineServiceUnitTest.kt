package com.popcorn.payment.service

import com.popcorn.payment.exception.PaymentException
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.*
import org.springframework.web.reactive.function.client.WebClient.*
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderQueryCoroutineService 단위 테스트")
class OrderQueryCoroutineServiceUnitTest {

    private val webClient = mockk<WebClient>()
    private val orderServiceBaseUrl = "http://localhost:8084"
    private val orderServiceTimeout = Duration.ofSeconds(30)

    private lateinit var orderQueryService: OrderQueryCoroutineService

    private val orderId = UUID.randomUUID()
    private val orderNo = "ORDER-12345"
    private val customerId = 123L
    private val totalAmount = 25000
    private val orderType = "DELIVERY"
    private val status = "PAID"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        orderQueryService = OrderQueryCoroutineService(
            webClient = webClient,
            orderServiceBaseUrl = orderServiceBaseUrl,
            orderServiceTimeout = orderServiceTimeout
        )
    }

    @Test
    @DisplayName("주문 조회 성공")
    fun `getOrder should successfully retrieve order information`() = runTest {
        // Given
        val orderDetailResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = status,
            totalAmount = totalAmount,
            createdAt = LocalDateTime.now()
        )

        val apiResponse = ApiResponse(
            code = 200,
            message = "success",
            data = orderDetailResponse
        )

        val requestHeadersUriSpec = mockk<RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mockk<RequestHeadersSpec<*>>()
        val responseSpec = mockk<ResponseSpec>()

        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri("$orderServiceBaseUrl/api/orders/v1/$orderId") } returns requestHeadersSpec
        every { requestHeadersSpec.header("X-Internal-Service", "payment-service") } returns requestHeadersSpec
        every { requestHeadersSpec.header("X-Internal-Call", "true") } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        coEvery { responseSpec.awaitBody<ApiResponse<OrderDetailApiResponse>>() } returns apiResponse

        // When
        val result = orderQueryService.getOrder(orderId)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.id)
        assertEquals(orderNo, result.orderNo)
        assertEquals(customerId, result.customerId)
        assertEquals(totalAmount, result.totalAmount)
        assertEquals(status, result.status)
        assertEquals(orderType, result.orderType)
        assertEquals(orderDetailResponse.createdAt, result.createdAt)

        verify(exactly = 1) { webClient.get() }
        coVerify(exactly = 1) { responseSpec.awaitBody<ApiResponse<OrderDetailApiResponse>>() }
    }

    @Test
    @DisplayName("주문 조회 성공 - createdAt이 null인 경우")
    fun `getOrder should handle null createdAt with current time`() = runTest {
        // Given
        val orderDetailResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = status,
            totalAmount = totalAmount,
            createdAt = null
        )

        val apiResponse = ApiResponse(data = orderDetailResponse)

        setupSuccessfulWebClientCall(apiResponse)

        // When
        val result = orderQueryService.getOrder(orderId)

        // Then
        assertNotNull(result)
        assertNotNull(result.createdAt)
        // createdAt should be around current time (within 1 second)
        assertTrue(result.createdAt.isAfter(LocalDateTime.now().minusSeconds(1)))
    }

    @Test
    @DisplayName("주문 조회 실패 - 데이터가 null")
    fun `getOrder should throw exception when response data is null`() = runTest {
        // Given
        val apiResponse = ApiResponse<OrderDetailApiResponse>(
            code = 404,
            message = "Order not found",
            data = null
        )

        setupSuccessfulWebClientCall(apiResponse)

        // When & Then
        val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
            runTest { orderQueryService.getOrder(orderId) }
        }
        assertTrue(exception.message!!.contains("주문을 찾을 수 없습니다"))
        assertTrue(exception.message!!.contains(orderId.toString()))
    }

    @Test
    @DisplayName("주문 조회 실패 - WebClientResponseException")
    fun `getOrder should handle WebClientResponseException`() = runTest {
        // Given
        val errorMessage = "Order service unavailable"
        val webClientException = WebClientResponseException.create(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            null,
            errorMessage.toByteArray(),
            null
        )

        setupWebClientCallWithException(webClientException)

        // When & Then
        val exception = assertThrows(PaymentException.ExternalApiError::class.java) {
            runTest { orderQueryService.getOrder(orderId) }
        }
        assertTrue(exception.message!!.contains("주문 조회 실패"))
        assertTrue(exception.message!!.contains("503 Service Unavailable"))
    }

    @Test
    @DisplayName("주문 조회 실패 - 일반 예외")
    fun `getOrder should handle general exception`() = runTest {
        // Given
        val errorMessage = "Connection timeout"
        val generalException = RuntimeException(errorMessage)

        setupWebClientCallWithException(generalException)

        // When & Then
        val exception = assertThrows(PaymentException.ExternalApiError::class.java) {
            runTest { orderQueryService.getOrder(orderId) }
        }
        assertTrue(exception.message!!.contains("주문 조회 실패"))
        assertTrue(exception.message!!.contains(errorMessage))
    }

    @Test
    @DisplayName("주문 상태 업데이트 성공")
    fun `updateOrderStatus should successfully update order status`() = runTest {
        // Given
        val newStatus = "COMPLETED"
        val reason = "결제 완료"

        // 상태 업데이트 요청 설정
        setupSuccessfulStatusUpdate(newStatus, reason)

        // 업데이트 후 주문 조회 설정
        val updatedOrderResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = newStatus,
            totalAmount = totalAmount,
            createdAt = LocalDateTime.now()
        )
        setupSuccessfulWebClientCall(ApiResponse(data = updatedOrderResponse))

        // When
        val result = orderQueryService.updateOrderStatus(orderId, newStatus, reason)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.id)
        assertEquals(newStatus, result.status)
        assertEquals(totalAmount, result.totalAmount)

        // 상태 업데이트와 조회가 모두 호출되어야 함
        verify(atLeast = 1) { webClient.patch() }
        verify(atLeast = 1) { webClient.get() }
    }

    @Test
    @DisplayName("주문 상태 업데이트 실패 - WebClientResponseException")
    fun `updateOrderStatus should handle WebClientResponseException`() = runTest {
        // Given
        val newStatus = "CANCELLED"
        val reason = "고객 요청"
        val errorResponse = "Invalid status transition"

        val webClientException = WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            null,
            errorResponse.toByteArray(),
            null
        )

        setupStatusUpdateWithException(webClientException)

        // When & Then
        val exception = assertThrows(PaymentException.ExternalApiError::class.java) {
            runTest { orderQueryService.updateOrderStatus(orderId, newStatus, reason) }
        }
        assertTrue(exception.message!!.contains("주문 상태 업데이트 실패"))
        assertTrue(exception.message!!.contains("400 Bad Request"))
    }

    @Test
    @DisplayName("주문 상태 업데이트 실패 - 일반 예외")
    fun `updateOrderStatus should handle general exception`() = runTest {
        // Given
        val newStatus = "FAILED"
        val reason = "시스템 오류"
        val errorMessage = "Network timeout"

        val generalException = RuntimeException(errorMessage)
        setupStatusUpdateWithException(generalException)

        // When & Then
        val exception = assertThrows(PaymentException.ExternalApiError::class.java) {
            runTest { orderQueryService.updateOrderStatus(orderId, newStatus, reason) }
        }
        assertTrue(exception.message!!.contains("주문 상태 업데이트 실패"))
        assertTrue(exception.message!!.contains(errorMessage))
    }

    @ParameterizedTest
    @ValueSource(strings = ["PENDING", "PAID", "COMPLETED", "CANCELLED", "FAILED"])
    @DisplayName("다양한 주문 상태에 대한 조회")
    fun `getOrder should handle various order statuses`(orderStatus: String) = runTest {
        // Given
        val orderDetailResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = orderStatus,
            totalAmount = totalAmount,
            createdAt = LocalDateTime.now()
        )

        setupSuccessfulWebClientCall(ApiResponse(data = orderDetailResponse))

        // When
        val result = orderQueryService.getOrder(orderId)

        // Then
        assertEquals(orderStatus, result.status)
    }

    @ParameterizedTest
    @ValueSource(strings = ["DELIVERY", "PICKUP", "DINE_IN"])
    @DisplayName("다양한 주문 타입에 대한 조회")
    fun `getOrder should handle various order types`(orderTypeValue: String) = runTest {
        // Given
        val orderDetailResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderTypeValue,
            status = status,
            totalAmount = totalAmount,
            createdAt = LocalDateTime.now()
        )

        setupSuccessfulWebClientCall(ApiResponse(data = orderDetailResponse))

        // When
        val result = orderQueryService.getOrder(orderId)

        // Then
        assertEquals(orderTypeValue, result.orderType)
    }

    @ParameterizedTest
    @ValueSource(ints = [1000, 5000, 10000, 50000, 100000])
    @DisplayName("다양한 금액에 대한 주문 조회")
    fun `getOrder should handle various order amounts`(amount: Int) = runTest {
        // Given
        val orderDetailResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = status,
            totalAmount = amount,
            createdAt = LocalDateTime.now()
        )

        setupSuccessfulWebClientCall(ApiResponse(data = orderDetailResponse))

        // When
        val result = orderQueryService.getOrder(orderId)

        // Then
        assertEquals(amount, result.totalAmount)
    }

    @Test
    @DisplayName("URI 빌드 및 인코딩 테스트 - 한글 사유")
    fun `updateOrderStatus should properly encode Korean reason`() = runTest {
        // Given
        val newStatus = "CANCELLED"
        val koreanReason = "고객 변심으로 인한 취소"

        setupSuccessfulStatusUpdate(newStatus, koreanReason)

        val updatedOrderResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = newStatus,
            totalAmount = totalAmount,
            createdAt = LocalDateTime.now()
        )
        setupSuccessfulWebClientCall(ApiResponse(data = updatedOrderResponse))

        // When
        val result = orderQueryService.updateOrderStatus(orderId, newStatus, koreanReason)

        // Then
        assertNotNull(result)
        assertEquals(newStatus, result.status)

        // URI 빌더가 호출되었는지 검증
        verify(exactly = 1) { webClient.patch() }
    }

    @Test
    @DisplayName("타임아웃 테스트 - 긴 응답 시간")
    fun `getOrder should handle timeout scenarios`() = runTest {
        // Given
        val timeoutException = kotlinx.coroutines.TimeoutCancellationException("Timed out waiting for 30000 ms")

        setupWebClientCallWithException(timeoutException)

        // When & Then
        val exception = assertThrows(PaymentException.ExternalApiError::class.java) {
            runTest { orderQueryService.getOrder(orderId) }
        }
        assertTrue(exception.message!!.contains("주문 조회 실패"))
    }

    @Test
    @DisplayName("내부 서비스 헤더 검증")
    fun `getOrder should set internal service headers correctly`() = runTest {
        // Given
        val orderDetailResponse = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = status,
            totalAmount = totalAmount,
            createdAt = LocalDateTime.now()
        )

        val apiResponse = ApiResponse(data = orderDetailResponse)

        val requestHeadersUriSpec = mockk<RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mockk<RequestHeadersSpec<*>>()
        val responseSpec = mockk<ResponseSpec>()

        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header("X-Internal-Service", "payment-service") } returns requestHeadersSpec
        every { requestHeadersSpec.header("X-Internal-Call", "true") } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        coEvery { responseSpec.awaitBody<ApiResponse<OrderDetailApiResponse>>() } returns apiResponse

        // When
        orderQueryService.getOrder(orderId)

        // Then
        verify(exactly = 1) { requestHeadersSpec.header("X-Internal-Service", "payment-service") }
        verify(exactly = 1) { requestHeadersSpec.header("X-Internal-Call", "true") }
    }

    @Test
    @DisplayName("빈 응답 데이터 처리")
    fun `getOrder should handle empty response gracefully`() = runTest {
        // Given
        val apiResponse = ApiResponse<OrderDetailApiResponse>()

        setupSuccessfulWebClientCall(apiResponse)

        // When & Then
        val exception = assertThrows(PaymentException.InvalidRequest::class.java) {
            runTest { orderQueryService.getOrder(orderId) }
        }
        assertTrue(exception.message!!.contains("주문을 찾을 수 없습니다"))
    }

    // 헬퍼 메서드들
    private fun setupSuccessfulWebClientCall(apiResponse: ApiResponse<OrderDetailApiResponse>) {
        val requestHeadersUriSpec = mockk<RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mockk<RequestHeadersSpec<*>>()
        val responseSpec = mockk<ResponseSpec>()

        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header(any(), any()) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        coEvery { responseSpec.awaitBody<ApiResponse<OrderDetailApiResponse>>() } returns apiResponse
    }

    private fun setupWebClientCallWithException(exception: Exception) {
        val requestHeadersUriSpec = mockk<RequestHeadersUriSpec<*>>()
        val requestHeadersSpec = mockk<RequestHeadersSpec<*>>()
        val responseSpec = mockk<ResponseSpec>()

        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header(any(), any()) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        coEvery { responseSpec.awaitBody<ApiResponse<OrderDetailApiResponse>>() } throws exception
    }

    private fun setupSuccessfulStatusUpdate(status: String, reason: String) {
        val requestBodyUriSpec = mockk<RequestBodyUriSpec>()
        val requestHeadersSpec = mockk<RequestHeadersSpec<*>>()
        val responseSpec = mockk<ResponseSpec>()

        every { webClient.patch() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<((UriBuilder) -> URI)>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header(any(), any()) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        coEvery { responseSpec.awaitBody<ApiResponse<Unit>>() } returns ApiResponse(code = 200, message = "success")
    }

    private fun setupStatusUpdateWithException(exception: Exception) {
        val requestBodyUriSpec = mockk<RequestBodyUriSpec>()
        val requestHeadersSpec = mockk<RequestHeadersSpec<*>>()
        val responseSpec = mockk<ResponseSpec>()

        every { webClient.patch() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<((UriBuilder) -> URI)>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header(any(), any()) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        coEvery { responseSpec.awaitBody<ApiResponse<Unit>>() } throws exception
    }
}
package com.popcorn.payment.service

import com.popcorn.payment.config.ReadOnlyOperation
import com.popcorn.payment.exception.PaymentException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.nio.charset.StandardCharsets

/**
 * 주문 조회 서비스 (코루틴 버전)
 *
 * 🔍 주요 기능:
 * - 주문 정보 조회 (읽기 전용)
 * - 주문 상태 업데이트
 * - 결제와 연동된 주문 데이터 제공
 *
 * 📊 코루틴 최적화:
 * - 읽기 전용 트랜잭션으로 성능 향상
 * - withContext(Dispatchers.IO)로 블로킹 작업 격리
 * - 캐싱 전략 적용 가능 (추후 확장)
 */
@Service
class OrderQueryCoroutineService(
    private val webClient: WebClient,
    @param:Value("\${microservices.order.base-url}")
    private val orderServiceBaseUrl: String,
    @param:Value("\${microservices.order.timeout:30s}")
    private val orderServiceTimeout: Duration
) {

    private val log = LoggerFactory.getLogger(OrderQueryCoroutineService::class.java)

    /**
     * 주문 ID로 주문 정보 조회
     *
     * 🎯 코루틴 최적화:
     * - 읽기 전용 트랜잭션 사용
     * - IO 스레드에서 DB 조회 실행
     * - 캐시 적중률 향상을 위한 구조
     *
     * @param orderId 주문 ID
     * @return 주문 정보
     */
    @ReadOnlyOperation
    suspend fun getOrder(orderId: UUID): OrderInfo {
        log.debug("🔍 주문 조회: orderId={}", orderId)

        return try {
            val response = withTimeout(orderServiceTimeout.toMillis()) {
                webClient
                    .get()
                    .uri("$orderServiceBaseUrl/api/orders/v1/$orderId")
                    .header("X-Internal-Service", "payment-service")
                    .header("X-Internal-Call", "true")
                    .retrieve()
                    .awaitBody<ApiResponse<OrderDetailApiResponse>>()
            }

            val data = response.data
                ?: throw PaymentException.invalidRequest("주문을 찾을 수 없습니다: $orderId")

            OrderInfo(
                id = data.orderId,
                orderNo = data.orderNo,
                customerId = data.customerId,
                totalAmount = data.totalAmount,
                status = data.status,
                orderType = data.orderType,
                createdAt = data.createdAt ?: LocalDateTime.now()
            )
        } catch (e: WebClientResponseException) {
            log.error("주문 조회 실패: orderId={}, status={}, error={}",
                orderId, e.statusCode, e.responseBodyAsString, e)
            throw PaymentException.externalApiError("주문 조회 실패: ${e.message}")
        } catch (e: Exception) {
            log.error("주문 조회 실패: orderId={}, error={}", orderId, e.message, e)
            throw PaymentException.externalApiError("주문 조회 실패: ${e.message}")
        }
    }

    /**
     * 주문 상태 업데이트
     *
     * @param orderId 주문 ID
     * @param status 새로운 상태
     * @param reason 상태 변경 사유
     * @return 업데이트된 주문 정보
     */
    suspend fun updateOrderStatus(
        orderId: UUID,
        status: String,
        reason: String
    ): OrderInfo {
        log.info("🔄 주문 상태 업데이트: orderId={}, status={}, reason={}", orderId, status, reason)

        return try {
            withTimeout(orderServiceTimeout.toMillis()) {
                webClient
                    .patch()
                    .uri { uriBuilder ->
                        UriComponentsBuilder
                            .fromHttpUrl(orderServiceBaseUrl)
                            .path("/api/orders/v1/$orderId/status")
                            .queryParam("status", status)
                            .queryParam("reason", reason)
                            .encode(StandardCharsets.UTF_8)
                            .build()
                            .toUri()
                    }
                    .header("X-Internal-Service", "payment-service")
                    .header("X-Internal-Call", "true")
                    .retrieve()
                    .awaitBody<ApiResponse<Unit>>()
            }

            getOrder(orderId)
        } catch (e: WebClientResponseException) {
            log.error("주문 상태 업데이트 실패: orderId={}, status={}, error={}",
                orderId, status, e.responseBodyAsString, e)
            throw PaymentException.externalApiError("주문 상태 업데이트 실패: ${e.message}")
        } catch (e: Exception) {
            log.error("주문 상태 업데이트 실패: orderId={}, status={}, error={}", orderId, status, e.message, e)
            throw PaymentException.externalApiError("주문 상태 업데이트 실패: ${e.message}")
        }
    }

}

/**
 * 주문 정보 DTO
 */
data class OrderInfo(
    val id: UUID,
    val orderNo: String,
    val customerId: Long,
    val totalAmount: Int,
    val status: String,
    val orderType: String,
    val createdAt: LocalDateTime
)

data class ApiResponse<T>(
    val code: Int? = null,
    val message: String? = null,
    val data: T? = null
)

data class PageResponse<T>(
    val content: List<T> = emptyList()
)

data class OrderDetailApiResponse(
    val orderId: UUID,
    val orderNo: String,
    val customerId: Long,
    val orderType: String,
    val status: String,
    val totalAmount: Int,
    val createdAt: LocalDateTime? = null
)

data class OrderSummaryApiResponse(
    val orderId: UUID,
    val orderNo: String,
    val customerId: Long,
    val orderType: String,
    val status: String,
    val totalAmount: Int,
    val createdAt: LocalDateTime? = null
)

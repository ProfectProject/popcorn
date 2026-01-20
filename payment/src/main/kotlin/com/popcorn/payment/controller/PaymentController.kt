package com.popcorn.payment.controller

import com.popcorn.payment.dto.*
import com.popcorn.payment.exception.PaymentException
import com.popcorn.payment.service.TossPaymentCoroutineService
import com.popcorn.payment.service.PaymentCommandCoroutineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * 결제 API 컨트롤러 (코루틴 기반)
 */
@Tag(name = "Payment API", description = "결제 관리 API")
@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"])
class PaymentController(
    private val tossPaymentService: TossPaymentCoroutineService,
    private val paymentCommandService: PaymentCommandCoroutineService
) {

    private val log = LoggerFactory.getLogger(PaymentController::class.java)

    /**
     * 토스페이먼츠 결제 승인
     */
    @Operation(
        summary = "결제 승인",
        description = "토스페이먼츠를 통한 결제 승인을 처리합니다. 멱등성이 보장되어 중복 요청 시 동일한 결과를 반환합니다."
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "결제 승인 성공"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        SwaggerApiResponse(responseCode = "500", description = "서버 내부 오류")
    )
    @PostMapping("/confirm")
    suspend fun confirmPayment(
        @Valid @RequestBody
        @Parameter(description = "결제 승인 요청 정보", required = true)
        request: PaymentConfirmRequest
    ): ResponseEntity<ApiResponse<PaymentConfirmResponse>> = coroutineScope {

        log.info("💳 결제 승인 요청: paymentKey={}, orderId={}, amount={}원",
            request.paymentKey, request.orderId, request.amount)

        try {
            val result = tossPaymentService.confirmPayment(
                paymentKey = request.paymentKey,
                orderId = request.orderId,
                amount = request.amount
            )

            val response = PaymentConfirmResponse(
                paymentId = result.paymentId,
                paymentStatus = result.paymentStatus,
                orderStatus = result.orderStatus,
                orderId = result.orderId,
                orderNo = result.orderNo,
                amount = result.amount,
                approvedAt = result.approvedAt
            )

            log.info("✅ 결제 승인 성공: paymentId={}, amount={}원", result.paymentId, result.amount)
            ResponseEntity.ok(ApiResponse.success(response, "결제가 성공적으로 승인되었습니다."))

        } catch (e: PaymentException) {
            log.error("❌ 결제 승인 실패: paymentKey={}, error={}", request.paymentKey, e.message)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.message ?: "결제 승인에 실패했습니다."))

        } catch (e: Exception) {
            log.error("❌ 결제 승인 중 예외 발생: paymentKey={}", request.paymentKey, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        }
    }

    /**
     * 결제 취소
     */
    @PostMapping("/{paymentId}/cancel")
    suspend fun cancelPayment(
        @PathVariable paymentId: UUID,
        @Valid @RequestBody request: PaymentCancelRequest
    ): ResponseEntity<ApiResponse<PaymentCancelResponse>> {

        log.info("🔄 결제 취소 요청: paymentId={}, reason={}", paymentId, request.cancelReason)

        try {
            // 결제 정보로부터 주문 ID 조회
            val paymentDetail = paymentCommandService.getLatestPaymentByOrderId(paymentId) // 임시로 paymentId 사용

            val result = tossPaymentService.cancelPayment(
                orderId = paymentDetail.orderId ?: paymentId, // 임시 처리
                cancelReason = request.cancelReason
            )

            val response = PaymentCancelResponse(
                paymentId = result.paymentId,
                orderId = result.orderId,
                cancelAmount = result.cancelAmount,
                status = result.status,
                cancelReason = result.cancelReason
            )

            log.info("✅ 결제 취소 성공: paymentId={}, cancelAmount={}원", result.paymentId, result.cancelAmount)
            return ResponseEntity.ok(ApiResponse.success(response, "결제가 성공적으로 취소되었습니다."))

        } catch (e: PaymentException) {
            log.error("❌ 결제 취소 실패: paymentId={}, error={}", paymentId, e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.message ?: "결제 취소에 실패했습니다."))

        } catch (e: Exception) {
            log.error("❌ 결제 취소 중 예외 발생: paymentId={}", paymentId, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        }
    }

    /**
     * 결제 생성
     */
    @PostMapping
    suspend fun createPayment(
        @Valid @RequestBody request: PaymentCreateRequest
    ): ResponseEntity<ApiResponse<PaymentCreateResponse>> {

        log.info("📝 결제 생성 요청: orderId={}, method={}, amount={}원",
            request.orderId, request.paymentMethod, request.amount)

        try {
            val result = paymentCommandService.createPayment(
                orderId = request.orderId,
                paymentMethod = request.paymentMethod,
                amount = request.amount,
                rawPayload = ""
            )

            val response = PaymentCreateResponse(
                paymentId = result.paymentId,
                orderId = request.orderId,
                amount = result.amount,
                status = result.status,
                paymentMethod = request.paymentMethod,
                createdAt = result.createdAt
            )

            log.info("✅ 결제 생성 성공: paymentId={}", result.paymentId)
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "결제가 성공적으로 생성되었습니다."))

        } catch (e: PaymentException) {
            log.error("❌ 결제 생성 실패: orderId={}, error={}", request.orderId, e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.message ?: "결제 생성에 실패했습니다."))

        } catch (e: Exception) {
            log.error("❌ 결제 생성 중 예외 발생: orderId={}", request.orderId, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        }
    }

    /**
     * 결제 상세 조회
     */
    @GetMapping("/{paymentId}")
    suspend fun getPayment(
        @PathVariable paymentId: UUID
    ): ResponseEntity<ApiResponse<PaymentDetailResponse>> {

        log.debug("🔍 결제 조회 요청: paymentId={}", paymentId)

        try {
            // 임시로 구현 (실제로는 별도 조회 서비스 필요)
            val result = paymentCommandService.getLatestPaymentByOrderId(paymentId)

            val response = PaymentDetailResponse(
                paymentId = result.paymentId,
                orderId = result.orderId ?: UUID.randomUUID(),
                amount = result.amount,
                status = result.status,
                paymentMethod = "CARD", // 임시값
                createdAt = java.time.LocalDateTime.now(),
                approvedAt = result.approvedAt,
                updatedAt = java.time.LocalDateTime.now()
            )

            return ResponseEntity.ok(ApiResponse.success(response))

        } catch (e: PaymentException) {
            log.error("❌ 결제 조회 실패: paymentId={}, error={}", paymentId, e.message)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.message ?: "결제 정보를 찾을 수 없습니다."))

        } catch (e: Exception) {
            log.error("❌ 결제 조회 중 예외 발생: paymentId={}", paymentId, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        }
    }

    /**
     * 주문의 결제 목록 조회
     */
    @GetMapping("/orders/{orderId}")
    suspend fun getPaymentsByOrderId(
        @PathVariable orderId: UUID
    ): ResponseEntity<ApiResponse<PaymentListResponse>> = coroutineScope {

        log.debug("🔍 주문 결제 목록 조회: orderId={}", orderId)

        try {
            // 비동기로 결제 목록과 통계 조회
            val paymentsDeferred = async {
                // 임시 구현 - 실제로는 PaymentRepository의 조회 메서드 사용
                listOf<PaymentDetailResponse>()
            }

            val statsDeferred = async {
                // 결제 통계 계산
                Pair(0, 0L) // (건수, 총금액)
            }

            val payments = paymentsDeferred.await()
            val (totalCount, totalAmount) = statsDeferred.await()

            val response = PaymentListResponse(
                payments = payments,
                totalCount = totalCount,
                totalAmount = totalAmount
            )

            ResponseEntity.ok(ApiResponse.success(response))

        } catch (e: Exception) {
            log.error("❌ 주문 결제 목록 조회 실패: orderId={}", orderId, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("시스템 오류가 발생했습니다."))
        }
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    suspend fun healthCheck(): ResponseEntity<Map<String, Any>> {
        val healthInfo = mapOf(
            "status" to "UP",
            "timestamp" to java.time.LocalDateTime.now(),
            "service" to "payment-service"
        )
        return ResponseEntity.ok(healthInfo)
    }
}